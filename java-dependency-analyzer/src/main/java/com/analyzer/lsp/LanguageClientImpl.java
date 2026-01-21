package com.analyzer.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the Language Client that receives notifications from JDT LS.
 */
public class LanguageClientImpl implements LanguageClient {
    private static final Logger logger = LoggerFactory.getLogger(LanguageClientImpl.class);

    @Override
    public void telemetryEvent(Object object) {
        logger.debug("Telemetry event: {}", object);
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        logger.debug("Diagnostics for {}: {} issues",
                diagnostics.getUri(), diagnostics.getDiagnostics().size());
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        logger.info("Server message [{}]: {}",
                messageParams.getType(), messageParams.getMessage());
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        logger.info("Server message request: {}", requestParams.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams message) {
        String msg = message.getMessage();
        
        // Downgrade certain non-critical errors to warnings
        if (msg != null && (msg.contains("Failed to import projects") || 
                           msg.contains("Cannot nest") ||
                           msg.contains("Java Model Exception"))) {
            logger.warn("JDT LS (non-critical): {}", msg);
            return;
        }
        
        switch (message.getType()) {
            case Error:
                logger.error("JDT LS: {}", msg);
                break;
            case Warning:
                logger.warn("JDT LS: {}", msg);
                break;
            case Info:
                logger.info("JDT LS: {}", msg);
                break;
            case Log:
            default:
                logger.debug("JDT LS: {}", msg);
                break;
        }
    }

    /**
     * Handle language/status notifications from JDT LS.
     * This is a custom notification sent by JDT LS to report its status
     * (e.g., "Starting Java Language Server", "Building", etc.)
     */
    @JsonNotification("language/status")
    public void languageStatus(Object params) {
        // Log at debug level to avoid spam, or comment out to ignore completely
        logger.debug("JDT LS status: {}", params);
    }
}
