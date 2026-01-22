/**
 * Main application entry point.
 */

let visualizer;
let wsClient;

// Initialize the application when the page loads
document.addEventListener('DOMContentLoaded', () => {
    console.log('Initializing Java Dependency Graph Analyzer...');

    // Create the graph visualizer
    visualizer = new GraphVisualizer('canvas-container');

    // Construct WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/graph`;

    // Create WebSocket client
    wsClient = new WebSocketClient(wsUrl, (graphData) => {
        handleGraphData(graphData);
    });

    // Connect to server
    wsClient.connect();

    // Set up UI controls
    setupControls();

    console.log('Application initialized');
});

/**
 * Handle received graph data.
 */
function handleGraphData(graphData) {
    console.log('Processing graph data...', graphData);
    
    // Validate graphData exists before accessing properties
    if (!graphData) {
        console.error('Received null or undefined graph data');
        hideLoadingScreen();
        showEmptyGraphMessage();
        return;
    }
    
    console.log('Graph data type:', typeof graphData);
    console.log('Nodes:', graphData.nodes);
    console.log('Edges:', graphData.edges);

    // Hide loading screen
    hideLoadingScreen();

    // Check if graph is empty
    if (!graphData.nodes || graphData.nodes.length === 0) {
        console.warn('Received empty graph - no nodes found');
        console.warn('Graph data structure:', JSON.stringify(graphData, null, 2));
        showEmptyGraphMessage();
        return;
    }

    console.log(`Rendering graph with ${graphData.nodes.length} nodes and ${graphData.edges?.length || 0} edges`);

    // Render the graph
    if (visualizer) {
        visualizer.renderGraph(graphData);
    } else {
        console.error('Visualizer not initialized!');
    }
}

/**
 * Show a message when the graph is empty.
 */
function showEmptyGraphMessage() {
    const container = document.getElementById('canvas-container');
    if (container) {
        const message = document.createElement('div');
        message.id = 'empty-graph-message';
        message.style.cssText = `
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            text-align: center;
            color: #aaaaff;
            font-size: 18px;
            z-index: 10;
        `;
        message.innerHTML = `
            <h2>No Classes Found</h2>
            <p>The workspace appears to be empty or the analysis found no Java classes.</p>
            <p>Make sure you're analyzing a valid Java project.</p>
        `;
        container.appendChild(message);
    }
}

/**
 * Hide the loading screen.
 */
function hideLoadingScreen() {
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
        loadingElement.classList.add('hidden');
    }
}

/**
 * Set up UI controls.
 */
function setupControls() {
    // Reset camera button
    const resetCameraBtn = document.getElementById('reset-camera');
    if (resetCameraBtn) {
        resetCameraBtn.addEventListener('click', () => {
            if (visualizer) {
                visualizer.resetCamera();
            }
        });
    }

    // Toggle labels button
    const toggleLabelsBtn = document.getElementById('toggle-labels');
    if (toggleLabelsBtn) {
        toggleLabelsBtn.addEventListener('click', () => {
            if (visualizer) {
                visualizer.toggleLabels();
            }
        });
    }
}

/**
 * Clean up on page unload.
 */
window.addEventListener('beforeunload', () => {
    if (wsClient) {
        wsClient.close();
    }
});
