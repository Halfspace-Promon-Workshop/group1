/**
 * Main application entry point.
 */

let visualizer = null;
let wsClient = null;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== Java Dependency Graph Analyzer ===');
    console.log('DOM loaded, initializing...');

    // Create visualizer
    try {
        visualizer = new GraphVisualizer('canvas-container');
        console.log('Visualizer created');
    } catch (error) {
        console.error('Failed to create visualizer:', error);
        showLoadingError('Failed to initialize visualization: ' + error.message);
        return;
    }

    // Setup WebSocket
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = wsProtocol + '//' + window.location.host + '/ws/graph';
    console.log('WebSocket URL:', wsUrl);

    // Create WebSocket client
    wsClient = new WebSocketClient(wsUrl, function(graphData) {
        console.log('=== Graph data received ===');
        console.log('Nodes:', graphData.nodes ? graphData.nodes.length : 0);
        console.log('Edges:', graphData.edges ? graphData.edges.length : 0);

        hideLoading();

        if (!graphData.nodes || graphData.nodes.length === 0) {
            showEmptyGraph();
            return;
        }

        if (visualizer) {
            visualizer.renderGraph(graphData);
        }
    });

    wsClient.connect();

    // Setup controls
    setupControls();

    // Fallback timeout
    setTimeout(function() {
        const loading = document.getElementById('loading');
        if (loading && !loading.classList.contains('hidden')) {
            console.warn('Loading timeout reached');
            loading.innerHTML = '<p style="color:#ffa500;">Taking longer than expected...</p>';
            setTimeout(function() {
                loading.classList.add('hidden');
            }, 3000);
        }
    }, 15000);

    console.log('Initialization complete');
});

function showLoadingError(message) {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.innerHTML = '<p style="color:#ff6b6b;">' + message + '</p>';
    }
}

function hideLoading() {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.classList.add('hidden');
    }
}

function showEmptyGraph() {
    const container = document.getElementById('canvas-container');
    if (container) {
        const div = document.createElement('div');
        div.style.cssText = 'position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:#aaf;text-align:center;z-index:10;';
        div.innerHTML = '<h2>No Classes Found</h2><p>The workspace appears empty or no Java classes were found.</p>';
        container.appendChild(div);
    }
}

function setupControls() {
    const resetBtn = document.getElementById('reset-camera');
    if (resetBtn) {
        resetBtn.addEventListener('click', function() {
            if (visualizer) visualizer.resetCamera();
        });
    }

    const toggleBtn = document.getElementById('toggle-labels');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', function() {
            if (visualizer) visualizer.toggleLabels();
        });
    }
}

window.addEventListener('beforeunload', function() {
    if (wsClient) wsClient.close();
});
