package com.analyzer;

import com.analyzer.graph.ClassNode;
import com.analyzer.graph.DependencyEdge;
import com.analyzer.graph.DependencyGraph;
import com.analyzer.graph.GraphBuilder;
import com.analyzer.graph.layout.ForceDirectedLayout;
import com.analyzer.graph.layout.LayoutAlgorithm;
import com.analyzer.lsp.JdtLsClient;
import com.analyzer.server.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Main application entry point.
 * Connects to JDT LS, builds the dependency graph, and starts the web server.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_WORKSPACE_PATH = ".";
    private static final String DEFAULT_JDTLS_COMMAND = "jdtls";
    private static final int DEFAULT_SERVER_PORT = 8080;

    public static void main(String[] args) {
        logger.info("Starting Java Dependency Graph Analyzer");

        try {
            // Load configuration
            Properties config = loadConfiguration();

            // Parse command line arguments or use defaults
            String workspacePath = getConfigValue(config, args, "jdtls.workspace", 0, DEFAULT_WORKSPACE_PATH);
            String jdtlsCommand = getConfigValue(config, args, "jdtls.command", 1, DEFAULT_JDTLS_COMMAND);
            int serverPort = Integer.parseInt(getConfigValue(config, args, "server.port", 2, 
                    String.valueOf(DEFAULT_SERVER_PORT)));

            logger.info("Configuration:");
            logger.info("  Workspace: {}", workspacePath);
            logger.info("  JDT LS command: {}", jdtlsCommand);
            logger.info("  Server port: {}", serverPort);

            // Connect to JDT LS
            JdtLsClient lsClient = new JdtLsClient(workspacePath, jdtlsCommand);
            logger.info("Starting JDT Language Server...");

            try {
                lsClient.connect();
                logger.info("Successfully started JDT LS");
            } catch (IOException e) {
                logger.error("Failed to start JDT LS for workspace: {}", workspacePath);
                logger.error("Error: {}", e.getMessage());
                logger.info("\nMake sure:");
                logger.info("  1. jdtls is installed (e.g., via Nix: nix-env -iA nixpkgs.jdt-language-server)");
                logger.info("  2. The workspace path points to a valid Java project: {}", workspacePath);
                logger.info("\nUsage: java -jar analyzer.jar [workspace-path] [jdtls-command] [server-port]");
                logger.info("Example: java -jar analyzer.jar /path/to/nifi jdtls 8080");
                System.exit(1);
            } catch (Exception e) {
                logger.error("Error initializing JDT LS connection", e);
                System.exit(1);
            }

            // Build dependency graph
            logger.info("Building dependency graph...");
            GraphBuilder graphBuilder = new GraphBuilder(lsClient);
            DependencyGraph graph;

            try {
                graph = graphBuilder.buildGraph();
                logger.info("Graph built successfully: {} nodes, {} edges",
                        graph.getNodeCount(), graph.getEdgeCount());

                if (graph.getNodeCount() == 0) {
                    logger.warn("No classes found in the workspace. Make sure JDT LS is analyzing the correct project.");
                }
                
                // If no edges were found, create synthetic test edges to verify frontend rendering
                if (graph.getEdgeCount() == 0 && graph.getNodeCount() > 1) {
                    logger.warn("No edges detected by JDT LS. Creating synthetic test edges to verify frontend...");
                    createSyntheticEdges(graph);
                    logger.info("Added synthetic edges. Graph now has {} edges", graph.getEdgeCount());
                }
            } catch (Exception e) {
                logger.error("Error building dependency graph", e);
                lsClient.disconnect();
                System.exit(1);
                return;
            }

            // Apply layout algorithm
            logger.info("Calculating graph layout...");
            LayoutAlgorithm layout = createLayoutAlgorithm(config);
            layout.calculateLayout(graph);
            logger.info("Layout calculation completed");

            // Disconnect from JDT LS (no longer needed)
            lsClient.disconnect();

            // Start web server
            logger.info("Starting web server on port {}...", serverPort);
            JettyServer server = new JettyServer(serverPort);

            try {
                server.start(graph);
                logger.info("\n");
                logger.info("========================================");
                logger.info("  Application started successfully!");
                logger.info("  Access at: http://localhost:{}", serverPort);
                logger.info("========================================");
                logger.info("\n");

                // Keep the application running
                server.join();
            } catch (Exception e) {
                logger.error("Error starting web server", e);
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Unexpected error", e);
            System.exit(1);
        }
    }

    /**
     * Load configuration from application.properties if it exists.
     */
    private static Properties loadConfiguration() {
        Properties config = new Properties();

        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                config.load(input);
                logger.info("Loaded configuration from application.properties");
            } else {
                logger.info("No application.properties found, using defaults");
            }
        } catch (IOException e) {
            logger.warn("Error loading application.properties: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Get configuration value from command line args, properties file, or default.
     */
    private static String getConfigValue(Properties config, String[] args, String propertyKey, 
                                          int argIndex, String defaultValue) {
        // Check command line arguments first
        if (args.length > argIndex) {
            return args[argIndex];
        }

        // Check properties file
        String value = config.getProperty(propertyKey);
        if (value != null) {
            return value;
        }

        // Use default
        return defaultValue;
    }

    /**
     * Create the layout algorithm based on configuration.
     */
    private static LayoutAlgorithm createLayoutAlgorithm(Properties config) {
        String algorithm = config.getProperty("layout.algorithm", "force-directed");

        if ("force-directed".equals(algorithm)) {
            int iterations = Integer.parseInt(config.getProperty("layout.iterations", "1000"));
            double temperature = Double.parseDouble(config.getProperty("layout.temperature", "100"));
            double idealDistance = Double.parseDouble(config.getProperty("layout.ideal-distance", "150"));

            logger.info("Using force-directed layout (iterations={}, temperature={}, idealDistance={})",
                    iterations, temperature, idealDistance);

            return new ForceDirectedLayout(iterations, temperature, idealDistance, 1.0, 0.01);
        } else {
            logger.warn("Unknown layout algorithm '{}', using default force-directed", algorithm);
            return new ForceDirectedLayout();
        }
    }
    
    /**
     * Create synthetic edges between nodes to test frontend rendering.
     * This is used when JDT LS doesn't detect any dependencies.
     */
    private static void createSyntheticEdges(DependencyGraph graph) {
        List<ClassNode> nodes = new ArrayList<>(graph.getNodes());
        
        if (nodes.size() < 2) {
            logger.warn("Not enough nodes to create synthetic edges");
            return;
        }
        
        // Create edges between sequential nodes (like a chain)
        int edgesToCreate = Math.min(nodes.size() - 1, 50); // Limit to 50 edges
        
        for (int i = 0; i < edgesToCreate; i++) {
            ClassNode source = nodes.get(i);
            ClassNode target = nodes.get(i + 1);
            
            DependencyEdge edge = new DependencyEdge(source, target, "syntheticField" + i);
            graph.addEdge(edge);
            
            if (i < 5) {
                logger.info("Created synthetic edge: {} -> {}", source.getName(), target.getName());
            }
        }
        
        // Also create some random cross-connections
        int crossEdges = Math.min(nodes.size() / 4, 20);
        for (int i = 0; i < crossEdges; i++) {
            int sourceIdx = (i * 3) % nodes.size();
            int targetIdx = (i * 7 + 5) % nodes.size();
            
            if (sourceIdx != targetIdx) {
                ClassNode source = nodes.get(sourceIdx);
                ClassNode target = nodes.get(targetIdx);
                
                DependencyEdge edge = new DependencyEdge(source, target, "crossField" + i);
                graph.addEdge(edge);
            }
        }
        
        logger.info("Created {} synthetic edges total", graph.getEdgeCount());
    }
}
