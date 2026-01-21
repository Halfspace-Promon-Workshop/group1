package com.analyzer.server;

import com.analyzer.graph.DependencyGraph;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for streaming graph data to connected clients.
 */
@WebSocket
public class GraphWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(GraphWebSocketHandler.class);

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static DependencyGraph currentGraph;

    /**
     * Set the graph to be sent to clients.
     */
    public static void setGraph(DependencyGraph graph) {
        currentGraph = graph;
        broadcastGraph();
    }

    /**
     * Get the current graph.
     */
    public static DependencyGraph getCurrentGraph() {
        return currentGraph;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        logger.info("WebSocket client connected: {} (total: {})",
                session.getRemoteAddress(), sessions.size());

        // Send current graph to newly connected client
        // Always send, even if empty, so frontend knows connection is established
        if (currentGraph != null) {
            sendGraphToSession(session, currentGraph);
        } else {
            // Send empty graph so frontend can hide loading screen
            logger.info("No graph data available yet, sending empty graph to client");
            DependencyGraph emptyGraph = new DependencyGraph();
            sendGraphToSession(session, emptyGraph);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        logger.info("WebSocket client disconnected: {} - {} (total: {})",
                session.getRemoteAddress(), reason, sessions.size());
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        if (error != null) {
            logger.error("WebSocket error for client {}: {}",
                    session != null ? session.getRemoteAddress() : "unknown", 
                    error.getMessage(), error);
        } else {
            logger.error("WebSocket error for client {} - error is null",
                    session != null ? session.getRemoteAddress() : "unknown");
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        logger.debug("Received message from {}: {}", session.getRemoteAddress(), message);
        // For now, we just log received messages
        // Future: could handle commands like "refresh" or "filter"
    }

    /**
     * Send graph data to a specific session.
     */
    private static void sendGraphToSession(Session session, DependencyGraph graph) {
        if (session.isOpen()) {
            try {
                String json = graph.toJson();
                logger.info("Sending graph data to client: {} ({} nodes, {} edges, {} bytes)",
                        session.getRemoteAddress(), graph.getNodeCount(), graph.getEdgeCount(), json.length());
                session.getRemote().sendString(json);
                logger.info("Successfully sent graph data to client: {}", session.getRemoteAddress());
            } catch (IOException e) {
                logger.error("Error sending graph to client {}: {}",
                        session.getRemoteAddress(), e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Unexpected error sending graph to client {}: {}",
                        session.getRemoteAddress(), e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot send graph to client {} - session is not open", session.getRemoteAddress());
        }
    }

    /**
     * Broadcast the current graph to all connected clients.
     */
    private static void broadcastGraph() {
        if (currentGraph == null) {
            logger.warn("No graph to broadcast");
            return;
        }

        logger.info("Broadcasting graph to {} connected clients", sessions.size());

        for (Session session : sessions) {
            sendGraphToSession(session, currentGraph);
        }
    }

    /**
     * Get the number of connected clients.
     */
    public static int getConnectedClientCount() {
        return sessions.size();
    }
}
