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
    console.log('Processing graph data...');

    // Hide loading screen
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
        loadingElement.classList.add('hidden');
    }

    // Render the graph
    if (visualizer) {
        visualizer.renderGraph(graphData);
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
