package com.analyzer.lsp;

import org.eclipse.lsp4j.*;
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
        switch (message.getType()) {
            case Error:
                logger.error("JDT LS: {}", message.getMessage());
                break;
            case Warning:
                logger.warn("JDT LS: {}", message.getMessage());
                break;
            case Info:
                logger.info("JDT LS: {}", message.getMessage());
                break;
            case Log:
            default:
                logger.debug("JDT LS: {}", message.getMessage());
                break;
        }
    }
}
