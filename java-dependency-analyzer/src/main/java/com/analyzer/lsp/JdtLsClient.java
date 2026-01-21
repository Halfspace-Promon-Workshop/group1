package com.analyzer.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client for connecting to JDT Language Server via stdio.
 */
public class JdtLsClient {
    private static final Logger logger = LoggerFactory.getLogger(JdtLsClient.class);

    private final String workspacePath;
    private final String jdtlsCommand;
    private Process jdtlsProcess;
    private LanguageServer languageServer;
    private LanguageClientImpl languageClient;
    private boolean initialized = false;

    /**
     * Create a new JDT LS client.
     * 
     * @param workspacePath Path to the Java project workspace
     * @param jdtlsCommand Command to launch jdtls (e.g., "jdtls" or full path)
     */
    public JdtLsClient(String workspacePath, String jdtlsCommand) {
        this.workspacePath = workspacePath;
        this.jdtlsCommand = jdtlsCommand;
    }

    /**
     * Create a new JDT LS client with default jdtls command.
     * 
     * @param workspacePath Path to the Java project workspace
     */
    public JdtLsClient(String workspacePath) {
        this(workspacePath, "jdtls");
    }

    /**
     * Start JDT LS as a subprocess and initialize via stdio.
     */
    public void connect() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        logger.info("Starting JDT LS for workspace: {}", workspacePath);

        // Validate workspace path
        Path workspace = Paths.get(workspacePath);
        if (!Files.exists(workspace)) {
            throw new IOException("Workspace path does not exist: " + workspacePath);
        }
        if (!Files.isDirectory(workspace)) {
            throw new IOException("Workspace path is not a directory: " + workspacePath);
        }

        // Build jdtls command
        List<String> command = new ArrayList<>();
        command.add(jdtlsCommand);
        command.add("-data");
        command.add(workspacePath);

        logger.info("Launching JDT LS: {}", String.join(" ", command));

        // Start jdtls process
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT); // Show stderr for debugging
        
        try {
            jdtlsProcess = processBuilder.start();
        } catch (IOException e) {
            logger.error("Failed to start jdtls. Make sure 'jdtls' is in your PATH or specify the full path.");
            throw new IOException("Failed to start jdtls command: " + jdtlsCommand, e);
        }

        // Get stdin/stdout streams
        InputStream input = jdtlsProcess.getInputStream();
        OutputStream output = jdtlsProcess.getOutputStream();

        // Create language client
        languageClient = new LanguageClientImpl();

        // Launch LSP client
        Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(
                languageClient,
                input,
                output
        );

        languageServer = launcher.getRemoteProxy();
        
        // Start listening in a separate thread
        launcher.startListening();
        
        // Monitor process termination
        CompletableFuture.runAsync(() -> {
            try {
                int exitCode = jdtlsProcess.waitFor();
                if (exitCode != 0) {
                    logger.error("JDT LS process exited with code: {}", exitCode);
                }
            } catch (InterruptedException e) {
                logger.warn("JDT LS process monitoring interrupted", e);
            }
        });

        logger.info("JDT LS process started, initializing...");

        // Initialize the language server
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        
        // Set root URI to the workspace
        initParams.setRootUri("file://" + workspace.toAbsolutePath().toString());
        initParams.setWorkspaceFolders(List.of(
            new WorkspaceFolder("file://" + workspace.toAbsolutePath().toString(), 
                               workspace.getFileName().toString())
        ));

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
        InitializeResult initResult = initFuture.get(60, TimeUnit.SECONDS); // Increased timeout for first init

        logger.info("JDT LS initialized successfully");
        logger.info("Server capabilities: workspace={}, textDocument={}", 
                   initResult.getCapabilities().getWorkspaceSymbolProvider() != null,
                   initResult.getCapabilities().getDocumentSymbolProvider() != null);

        // Send initialized notification
        languageServer.initialized(new InitializedParams());
        initialized = true;
        
        // Give jdtls some time to analyze the workspace
        logger.info("Waiting for JDT LS to analyze workspace...");
        Thread.sleep(5000); // Wait 5 seconds for initial analysis
    }

    /**
     * Get all workspace symbols (classes, interfaces, etc.).
     */
    public List<SymbolInformation> getWorkspaceSymbols(String query) throws ExecutionException, InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Client not initialized");
        }

        WorkspaceSymbolParams params = new WorkspaceSymbolParams(query);
        CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> future =
                languageServer.getWorkspaceService().symbol(params);

        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> symbolsEither = future.get();
        List<SymbolInformation> result = new ArrayList<>();

        if (symbolsEither.isLeft()) {
            // We got a list of SymbolInformation
            result.addAll(symbolsEither.getLeft());
        } else {
            // We got a list of WorkspaceSymbol - convert each to SymbolInformation
            for (WorkspaceSymbol ws : symbolsEither.getRight()) {
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

        if (jdtlsProcess != null && jdtlsProcess.isAlive()) {
            try {
                logger.info("Terminating JDT LS process");
                jdtlsProcess.destroy();
                if (!jdtlsProcess.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("JDT LS process did not terminate gracefully, forcing...");
                    jdtlsProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for JDT LS process to terminate", e);
                jdtlsProcess.destroyForcibly();
            }
        }

        initialized = false;
        logger.info("Disconnected from JDT LS");
    }
}
