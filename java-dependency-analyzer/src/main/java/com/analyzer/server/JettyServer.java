package com.analyzer.server;

import com.analyzer.graph.DependencyGraph;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

/**
 * Jetty web server for serving the frontend and WebSocket endpoint.
 */
public class JettyServer {
    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private final int port;
    private Server server;

    public JettyServer(int port) {
        this.port = port;
    }

    /**
     * Start the Jetty server.
     */
    public void start(DependencyGraph graph) throws Exception {
        server = new Server();

        // Configure HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // Create servlet context
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(Duration.ofMinutes(10));

            // Add WebSocket endpoint
            wsContainer.addMapping("/ws/graph", (req, resp) -> new GraphWebSocketHandler());
        });

        // Serve static files from webapp directory
        ServletHolder staticHolder = new ServletHolder("static", DefaultServlet.class);

        // Try to find webapp directory
        URL webappUrl = getClass().getClassLoader().getResource("webapp");
        if (webappUrl != null) {
            String resourceBase = webappUrl.toExternalForm();
            staticHolder.setInitParameter("resourceBase", resourceBase);
            logger.info("Serving static files from: {}", resourceBase);
        } else {
            // Fallback to file system path
            String resourceBase = "src/main/webapp";
            staticHolder.setInitParameter("resourceBase", resourceBase);
            logger.info("Serving static files from: {}", resourceBase);
        }

        staticHolder.setInitParameter("dirAllowed", "false");
        staticHolder.setInitParameter("pathInfoOnly", "true");
        context.addServlet(staticHolder, "/*");

        // Set the initial graph
        GraphWebSocketHandler.setGraph(graph);

        // Start server
        server.start();
        logger.info("Jetty server started on port {}", port);
        logger.info("Access the application at: http://localhost:{}", port);
    }

    /**
     * Stop the server.
     */
    public void stop() throws Exception {
        if (server != null && server.isRunning()) {
            logger.info("Stopping Jetty server");
            server.stop();
        }
    }

    /**
     * Wait for the server to complete.
     */
    public void join() throws InterruptedException {
        if (server != null) {
            server.join();
        }
    }

    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    /**
     * Update the graph and broadcast to all connected clients.
     */
    public void updateGraph(DependencyGraph graph) {
        GraphWebSocketHandler.setGraph(graph);
    }
}
