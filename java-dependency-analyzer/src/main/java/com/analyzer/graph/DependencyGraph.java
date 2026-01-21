package com.analyzer.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the complete dependency graph of classes.
 * Thread-safe for concurrent access.
 */
public class DependencyGraph {
    private final Map<String, ClassNode> nodes;
    private final Set<DependencyEdge> edges;
    private final Gson gson;

    public DependencyGraph() {
        this.nodes = new ConcurrentHashMap<>();
        this.edges = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Add a class node to the graph.
     */
    public void addNode(ClassNode node) {
        nodes.put(node.getFullyQualifiedName(), node);
    }

    /**
     * Add a dependency edge to the graph.
     */
    public void addEdge(DependencyEdge edge) {
        edges.add(edge);
    }

    /**
     * Get a node by its fully qualified name.
     */
    public ClassNode getNode(String fullyQualifiedName) {
        return nodes.get(fullyQualifiedName);
    }

    /**
     * Get all nodes in the graph.
     */
    public Collection<ClassNode> getNodes() {
        return nodes.values();
    }

    /**
     * Get all edges in the graph.
     */
    public Set<DependencyEdge> getEdges() {
        return new HashSet<>(edges);
    }

    /**
     * Get the number of nodes in the graph.
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Get the number of edges in the graph.
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * Get edges connected to a specific node.
     */
    public List<DependencyEdge> getEdgesForNode(ClassNode node) {
        List<DependencyEdge> result = new ArrayList<>();
        for (DependencyEdge edge : edges) {
            if (edge.getSource().equals(node) || edge.getTarget().equals(node)) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * Get the number of dependencies (outgoing edges) for a node.
     */
    public int getDependencyCount(ClassNode node) {
        int count = 0;
        for (DependencyEdge edge : edges) {
            if (edge.getSource().equals(node)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Convert the graph to JSON format for transmission to frontend.
     */
    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "graph");

        // Convert nodes
        JsonArray nodesArray = new JsonArray();
        for (ClassNode node : nodes.values()) {
            JsonObject nodeObj = new JsonObject();
            nodeObj.addProperty("id", node.getFullyQualifiedName());
            nodeObj.addProperty("name", node.getName());
            nodeObj.addProperty("fullName", node.getFullyQualifiedName());
            nodeObj.addProperty("x", node.getX());
            nodeObj.addProperty("y", node.getY());
            nodeObj.addProperty("z", node.getZ());
            nodeObj.addProperty("dependencyCount", getDependencyCount(node));
            nodesArray.add(nodeObj);
        }
        root.add("nodes", nodesArray);

        // Convert edges
        JsonArray edgesArray = new JsonArray();
        for (DependencyEdge edge : edges) {
            JsonObject edgeObj = new JsonObject();
            edgeObj.addProperty("source", edge.getSource().getFullyQualifiedName());
            edgeObj.addProperty("target", edge.getTarget().getFullyQualifiedName());
            edgeObj.addProperty("fieldName", edge.getFieldName());
            edgesArray.add(edgeObj);
        }
        root.add("edges", edgesArray);

        return gson.toJson(root);
    }

    /**
     * Clear all nodes and edges from the graph.
     */
    public void clear() {
        nodes.clear();
        edges.clear();
    }

    @Override
    public String toString() {
        return "DependencyGraph{" +
                "nodeCount=" + nodes.size() +
                ", edgeCount=" + edges.size() +
                '}';
    }
}
