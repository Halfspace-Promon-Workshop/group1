package com.analyzer.graph;

import com.analyzer.lsp.JdtLsClient;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Builds a dependency graph by analyzing Java classes via JDT LS.
 * Creates edges when one class has an instance variable of another class type.
 */
public class GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    private final JdtLsClient lsClient;
    private final DependencyGraph graph;

    // Set of Java standard library package prefixes to exclude
    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "java.", "javax.", "jakarta.",
            "sun.", "com.sun.",
            "org.w3c.", "org.xml.", "org.omg."
    );

    public GraphBuilder(JdtLsClient lsClient) {
        this.lsClient = lsClient;
        this.graph = new DependencyGraph();
    }

    /**
     * Build the dependency graph from the workspace.
     */
    public DependencyGraph buildGraph() throws ExecutionException, InterruptedException {
        logger.info("Starting dependency graph analysis");

        // Get all workspace symbols
        // Note: JDT LS requires "*" (wildcard) query to return all symbols
        List<SymbolInformation> symbols = lsClient.getWorkspaceSymbols("*");
        logger.info("Found {} symbols in workspace", symbols.size());

        // Filter to only classes and interfaces
        List<SymbolInformation> classSymbols = new ArrayList<>();
        for (SymbolInformation symbol : symbols) {
            if (symbol.getKind() == SymbolKind.Class || symbol.getKind() == SymbolKind.Interface) {
                String fullName = getFullyQualifiedName(symbol);
                if (!shouldExclude(fullName)) {
                    classSymbols.add(symbol);
                }
            }
        }

        logger.info("Found {} classes/interfaces to analyze", classSymbols.size());

        // Create nodes for all classes
        Map<String, ClassNode> nodeMap = new HashMap<>();
        for (SymbolInformation symbol : classSymbols) {
            String fullName = getFullyQualifiedName(symbol);
            ClassNode node = new ClassNode(symbol.getName(), fullName);
            graph.addNode(node);
            nodeMap.put(fullName, node);
        }

        // Analyze each class for dependencies
        int processedCount = 0;
        for (SymbolInformation classSymbol : classSymbols) {
            try {
                analyzeClassDependencies(classSymbol, nodeMap);
                processedCount++;

                if (processedCount % 10 == 0) {
                    logger.info("Analyzed {}/{} classes", processedCount, classSymbols.size());
                }
            } catch (Exception e) {
                logger.warn("Error analyzing class {}: {}", classSymbol.getName(), e.getMessage());
            }
        }

        logger.info("Graph building completed: {} nodes, {} edges",
                graph.getNodeCount(), graph.getEdgeCount());

        return graph;
    }

    /**
     * Analyze a single class for instance variable dependencies.
     */
    private void analyzeClassDependencies(SymbolInformation classSymbol, Map<String, ClassNode> nodeMap)
            throws ExecutionException, InterruptedException {

        String uri = classSymbol.getLocation().getUri();
        String className = getFullyQualifiedName(classSymbol);

        // Get document symbols for this class
        List<Either<SymbolInformation, DocumentSymbol>> docSymbols = lsClient.getDocumentSymbols(uri);

        ClassNode sourceNode = nodeMap.get(className);
        if (sourceNode == null) {
            return;
        }

        // Process document symbols
        for (Either<SymbolInformation, DocumentSymbol> either : docSymbols) {
            if (either.isRight()) {
                DocumentSymbol docSymbol = either.getRight();
                processDocumentSymbol(docSymbol, sourceNode, nodeMap, uri);
            }
        }
    }

    /**
     * Recursively process document symbols to find fields.
     */
    private void processDocumentSymbol(DocumentSymbol symbol, ClassNode sourceNode,
                                        Map<String, ClassNode> nodeMap, String uri) {
        // Check if this is a field (instance variable)
        if (symbol.getKind() == SymbolKind.Field) {
            String fieldName = symbol.getName();
            String detail = symbol.getDetail();
            
            // Log field details for debugging
            logger.info("Found field '{}' in class '{}', detail: '{}'", 
                    fieldName, sourceNode.getName(), detail);
            
            String fieldType = extractTypeFromDetail(detail);
            
            if (fieldType == null) {
                logger.debug("Could not extract type from detail '{}' for field '{}'", detail, fieldName);
            } else if (shouldExclude(fieldType)) {
                logger.debug("Excluding field '{}' of type '{}' (standard library)", fieldName, fieldType);
            } else {
                // Try to find the target node
                ClassNode targetNode = findNodeByType(fieldType, nodeMap);

                if (targetNode == null) {
                    logger.debug("Could not find node for type '{}' (field '{}')", fieldType, fieldName);
                } else if (targetNode.equals(sourceNode)) {
                    logger.debug("Skipping self-reference for field '{}'", fieldName);
                } else {
                    // Create dependency edge
                    DependencyEdge edge = new DependencyEdge(sourceNode, targetNode, fieldName);
                    graph.addEdge(edge);
                    logger.info("Created edge: {} -> {} (field: {})",
                            sourceNode.getName(), targetNode.getName(), fieldName);
                }
            }
        }

        // Recursively process children
        if (symbol.getChildren() != null) {
            for (DocumentSymbol child : symbol.getChildren()) {
                processDocumentSymbol(child, sourceNode, nodeMap, uri);
            }
        }
    }

    /**
     * Extract type information from symbol detail string.
     * The detail usually contains the type, e.g., "String name" or "List<Item> items"
     * JDT LS may return different formats depending on version:
     * - "Type" (just the type)
     * - "Type fieldName" (type and field name)
     * - ": Type" (with colon prefix)
     */
    private String extractTypeFromDetail(String detail) {
        if (detail == null || detail.isEmpty()) {
            logger.debug("Detail is null or empty");
            return null;
        }

        String trimmed = detail.trim();
        logger.debug("Extracting type from detail: '{}'", trimmed);
        
        // Handle ": Type" format (common in some JDT LS versions)
        if (trimmed.startsWith(":")) {
            trimmed = trimmed.substring(1).trim();
        }
        
        // Handle "Type : defaultValue" format
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0) {
            trimmed = trimmed.substring(0, colonIndex).trim();
        }

        // Try to extract type from detail string
        // Format is usually: "Type fieldName" or just "Type"
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 0) {
            String type = parts[0];
            
            // If type looks like a field name (lowercase), try the second part
            if (parts.length > 1 && Character.isLowerCase(type.charAt(0)) && 
                Character.isUpperCase(parts[1].charAt(0))) {
                type = parts[1];
            }

            // Remove generic parameters for simplicity
            int genericStart = type.indexOf('<');
            if (genericStart > 0) {
                type = type.substring(0, genericStart);
            }

            // Remove array brackets
            type = type.replace("[]", "");
            
            // Remove any remaining special characters
            type = type.replaceAll("[^a-zA-Z0-9_.]", "");

            if (!type.isEmpty()) {
                logger.debug("Extracted type: '{}'", type);
                return type;
            }
        }

        logger.debug("Could not extract type from detail");
        return null;
    }

    /**
     * Find a node by type name, trying both simple and fully qualified names.
     */
    private ClassNode findNodeByType(String typeName, Map<String, ClassNode> nodeMap) {
        // Try exact match first
        ClassNode node = nodeMap.get(typeName);
        if (node != null) {
            return node;
        }

        // Try matching by simple name
        for (Map.Entry<String, ClassNode> entry : nodeMap.entrySet()) {
            if (entry.getValue().getName().equals(typeName)) {
                return entry.getValue();
            }
        }

        // Try matching as suffix (for cases where we have partial package name)
        for (Map.Entry<String, ClassNode> entry : nodeMap.entrySet()) {
            if (entry.getKey().endsWith("." + typeName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Get fully qualified name from symbol information.
     */
    private String getFullyQualifiedName(SymbolInformation symbol) {
        // Container name usually has the package
        if (symbol.getContainerName() != null && !symbol.getContainerName().isEmpty()) {
            return symbol.getContainerName() + "." + symbol.getName();
        }
        return symbol.getName();
    }

    /**
     * Check if a class should be excluded from the graph.
     */
    private boolean shouldExclude(String fullyQualifiedName) {
        for (String prefix : EXCLUDED_PACKAGES) {
            if (fullyQualifiedName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the built graph.
     */
    public DependencyGraph getGraph() {
        return graph;
    }
}
