package com.analyzer.graph.layout;

import com.analyzer.graph.DependencyGraph;

/**
 * Interface for graph layout algorithms.
 * Allows different layout strategies to be implemented and swapped.
 */
public interface LayoutAlgorithm {
    /**
     * Calculate positions for all nodes in the graph.
     * This method should update the x, y, z coordinates of each node.
     *
     * @param graph The dependency graph to layout
     */
    void calculateLayout(DependencyGraph graph);
}
