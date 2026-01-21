package com.analyzer.graph.layout;

import com.analyzer.graph.ClassNode;
import com.analyzer.graph.DependencyEdge;
import com.analyzer.graph.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implements a 3D force-directed graph layout algorithm based on Fruchterman-Reingold.
 * This creates a visually pleasing layout by simulating physical forces between nodes.
 */
public class ForceDirectedLayout implements LayoutAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(ForceDirectedLayout.class);

    private final int maxIterations;
    private final double initialTemperature;
    private final double idealDistance;
    private final double repulsionStrength;
    private final double attractionStrength;

    public ForceDirectedLayout() {
        this(1000, 100.0, 150.0, 1.0, 0.01);
    }

    public ForceDirectedLayout(int maxIterations, double initialTemperature, double idealDistance,
                                double repulsionStrength, double attractionStrength) {
        this.maxIterations = maxIterations;
        this.initialTemperature = initialTemperature;
        this.idealDistance = idealDistance;
        this.repulsionStrength = repulsionStrength;
        this.attractionStrength = attractionStrength;
    }

    @Override
    public void calculateLayout(DependencyGraph graph) {
        Collection<ClassNode> nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            logger.warn("Graph has no nodes to layout");
            return;
        }

        logger.info("Starting force-directed layout for {} nodes and {} edges",
                graph.getNodeCount(), graph.getEdgeCount());

        // Initialize positions randomly
        initializePositions(nodes);

        double temperature = initialTemperature;
        double coolingRate = initialTemperature / maxIterations;

        // Main iteration loop
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Calculate repulsive forces between all pairs of nodes
            calculateRepulsiveForces(nodes);

            // Calculate attractive forces along edges
            calculateAttractiveForces(graph);

            // Update positions based on forces and temperature
            updatePositions(nodes, temperature);

            // Cool down the temperature
            temperature = Math.max(temperature - coolingRate, 0.1);

            if (iteration % 100 == 0) {
                logger.debug("Layout iteration {} / {}, temperature: {}", iteration, maxIterations, temperature);
            }
        }

        logger.info("Force-directed layout completed");
    }

    /**
     * Initialize node positions randomly in 3D space.
     */
    private void initializePositions(Collection<ClassNode> nodes) {
        Random random = new Random(42); // Fixed seed for reproducibility
        double spread = idealDistance * Math.sqrt(nodes.size());

        for (ClassNode node : nodes) {
            node.setX((random.nextDouble() - 0.5) * spread);
            node.setY((random.nextDouble() - 0.5) * spread);
            node.setZ((random.nextDouble() - 0.5) * spread);
            node.setVx(0);
            node.setVy(0);
            node.setVz(0);
        }
    }

    /**
     * Calculate repulsive forces between all pairs of nodes.
     * Nodes repel each other to prevent overlap.
     */
    private void calculateRepulsiveForces(Collection<ClassNode> nodes) {
        List<ClassNode> nodeList = new ArrayList<>(nodes);

        for (int i = 0; i < nodeList.size(); i++) {
            ClassNode node1 = nodeList.get(i);

            for (int j = i + 1; j < nodeList.size(); j++) {
                ClassNode node2 = nodeList.get(j);

                double dx = node2.getX() - node1.getX();
                double dy = node2.getY() - node1.getY();
                double dz = node2.getZ() - node1.getZ();

                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < 0.01) {
                    distance = 0.01; // Prevent division by zero
                }

                // Calculate repulsive force (inverse square law)
                double force = repulsionStrength * (idealDistance * idealDistance) / distance;

                double fx = (dx / distance) * force;
                double fy = (dy / distance) * force;
                double fz = (dz / distance) * force;

                // Apply force to both nodes (Newton's third law)
                node1.setVx(node1.getVx() - fx);
                node1.setVy(node1.getVy() - fy);
                node1.setVz(node1.getVz() - fz);

                node2.setVx(node2.getVx() + fx);
                node2.setVy(node2.getVy() + fy);
                node2.setVz(node2.getVz() + fz);
            }
        }
    }

    /**
     * Calculate attractive forces along edges.
     * Connected nodes are pulled together.
     */
    private void calculateAttractiveForces(DependencyGraph graph) {
        for (DependencyEdge edge : graph.getEdges()) {
            ClassNode source = edge.getSource();
            ClassNode target = edge.getTarget();

            double dx = target.getX() - source.getX();
            double dy = target.getY() - source.getY();
            double dz = target.getZ() - source.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.01) {
                distance = 0.01;
            }

            // Calculate attractive force (spring force)
            double force = attractionStrength * (distance - idealDistance);

            double fx = (dx / distance) * force;
            double fy = (dy / distance) * force;
            double fz = (dz / distance) * force;

            // Apply force to both nodes
            source.setVx(source.getVx() + fx);
            source.setVy(source.getVy() + fy);
            source.setVz(source.getVz() + fz);

            target.setVx(target.getVx() - fx);
            target.setVy(target.getVy() - fy);
            target.setVz(target.getVz() - fz);
        }
    }

    /**
     * Update node positions based on accumulated forces.
     */
    private void updatePositions(Collection<ClassNode> nodes, double temperature) {
        for (ClassNode node : nodes) {
            // Calculate displacement magnitude
            double vx = node.getVx();
            double vy = node.getVy();
            double vz = node.getVz();

            double displacement = Math.sqrt(vx * vx + vy * vy + vz * vz);

            if (displacement > 0.01) {
                // Limit displacement by temperature
                double limited = Math.min(displacement, temperature);
                double scale = limited / displacement;

                // Update position
                node.setX(node.getX() + vx * scale);
                node.setY(node.getY() + vy * scale);
                node.setZ(node.getZ() + vz * scale);
            }

            // Reset velocities for next iteration
            node.setVx(0);
            node.setVy(0);
            node.setVz(0);
        }
    }
}
