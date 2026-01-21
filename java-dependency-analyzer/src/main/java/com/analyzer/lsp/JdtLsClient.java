package com.analyzer.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client for connecting to JDT Language Server via socket.
 */
public class JdtLsClient {
    private static final Logger logger = LoggerFactory.getLogger(JdtLsClient.class);

    private final String host;
    private final int port;
    private Socket socket;
    private LanguageServer languageServer;
    private LanguageClientImpl languageClient;
    private boolean initialized = false;

    public JdtLsClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connect to JDT LS via socket and initialize.
     */
    public void connect() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        logger.info("Connecting to JDT LS at {}:{}", host, port);

        // Connect to socket
        socket = new Socket(host, port);
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();

        // Create language client
        languageClient = new LanguageClientImpl();

        // Launch LSP client
        Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(
                languageClient,
                input,
                output
        );

        languageServer = launcher.getRemoteProxy();
        launcher.startListening();

        logger.info("Connected to JDT LS, initializing...");

        // Initialize the language server
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());

        // Set capabilities
        ClientCapabilities capabilities = new ClientCapabilities();
        WorkspaceClientCapabilities workspaceCapabilities = new WorkspaceClientCapabilities();
        workspaceCapabilities.setSymbol(new SymbolCapabilities());
        capabilities.setWorkspace(workspaceCapabilities);

        TextDocumentClientCapabilities textDocCapabilities = new TextDocumentClientCapabilities();
        textDocCapabilities.setDocumentSymbol(new DocumentSymbolCapabilities());
        textDocCapabilities.setDefinition(new DefinitionCapabilities());
        capabilities.setTextDocument(textDocCapabilities);

        initParams.setCapabilities(capabilities);

        // Send initialize request
        CompletableFuture<InitializeResult> initFuture = languageServer.initialize(initParams);
        InitializeResult initResult = initFuture.get(30, TimeUnit.SECONDS);

        logger.info("JDT LS initialized successfully");

        // Send initialized notification
        languageServer.initialized(new InitializedParams());
        initialized = true;
    }

    /**
     * Get all workspace symbols (classes, interfaces, etc.).
     */
    public List<SymbolInformation> getWorkspaceSymbols(String query) throws ExecutionException, InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Client not initialized");
        }

        WorkspaceSymbolParams params = new WorkspaceSymbolParams(query);
        CompletableFuture<List<Either<SymbolInformation, WorkspaceSymbol>>> future =
                languageServer.getWorkspaceService().symbol(params);

        List<Either<SymbolInformation, WorkspaceSymbol>> symbols = future.get();
        List<SymbolInformation> result = new ArrayList<>();

        for (Either<SymbolInformation, WorkspaceSymbol> either : symbols) {
            if (either.isLeft()) {
                result.add(either.getLeft());
            } else {
                // Convert WorkspaceSymbol to SymbolInformation
                WorkspaceSymbol ws = either.getRight();
                SymbolInformation si = new SymbolInformation();
                si.setName(ws.getName());
                si.setKind(ws.getKind());
                if (ws.getLocation().isLeft()) {
                    si.setLocation(ws.getLocation().getLeft());
                }
                result.add(si);
            }
        }

        return result;
    }

    /**
     * Get document symbols for a specific file (classes, methods, fields).
     */
    public List<Either<SymbolInformation, DocumentSymbol>> getDocumentSymbols(String uri)
            throws ExecutionException, InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Client not initialized");
        }

        DocumentSymbolParams params = new DocumentSymbolParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));

        CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> future =
                languageServer.getTextDocumentService().documentSymbol(params);

        return future.get();
    }

    /**
     * Get the definition location for a symbol at a specific position.
     */
    public List<? extends Location> getDefinition(String uri, int line, int character)
            throws ExecutionException, InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Client not initialized");
        }

        DefinitionParams params = new DefinitionParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        params.setPosition(new Position(line, character));

        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                languageServer.getTextDocumentService().definition(params);

        Either<List<? extends Location>, List<? extends LocationLink>> result = future.get();

        if (result.isLeft()) {
            return result.getLeft();
        } else {
            // Convert LocationLink to Location
            List<Location> locations = new ArrayList<>();
            for (LocationLink link : result.getRight()) {
                locations.add(new Location(link.getTargetUri(), link.getTargetRange()));
            }
            return locations;
        }
    }

    /**
     * Check if the client is connected and initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Disconnect from the language server.
     */
    public void disconnect() {
        if (languageServer != null) {
            try {
                logger.info("Shutting down JDT LS connection");
                languageServer.shutdown().get(5, TimeUnit.SECONDS);
                languageServer.exit();
            } catch (Exception e) {
                logger.error("Error shutting down language server", e);
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }

        initialized = false;
        logger.info("Disconnected from JDT LS");
    }
}
