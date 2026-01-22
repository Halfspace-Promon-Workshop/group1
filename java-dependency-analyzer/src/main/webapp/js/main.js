/**
 * Main application entry point.
 */

let visualizer = null;
let wsClient = null;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== Java Dependency Graph Analyzer ===');
    console.log('DOM loaded, initializing...');

    // Check Three.js
    if (typeof THREE === 'undefined') {
        console.error('THREE.js not loaded!');
        showLoadingError('Three.js failed to load. Please refresh the page.');
        return;
    }
    console.log('THREE.js version:', THREE.REVISION);

    // Create visualizer
    try {
        visualizer = new GraphVisualizer('canvas-container');
        console.log('Visualizer created');
    } catch (error) {
        console.error('Failed to create visualizer:', error);
        showLoadingError('Failed to initialize 3D visualization: ' + error.message);
        return;
    }

    // Setup WebSocket
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = wsProtocol + '//' + window.location.host + '/ws/graph';
    console.log('WebSocket URL:', wsUrl);

    // Create WebSocket client
    wsClient = new WebSocketClient(wsUrl, function(graphData) {
        console.log('=== Graph data received ===');
        console.log('Type:', graphData.type);
        console.log('Nodes:', graphData.nodes ? graphData.nodes.length : 0);
        console.log('Edges:', graphData.edges ? graphData.edges.length : 0);
        
        if (graphData.edges && graphData.edges.length > 0) {
            console.log('Sample edge:', graphData.edges[0]);
        }
        if (graphData.nodes && graphData.nodes.length > 0) {
            console.log('Sample node:', graphData.nodes[0]);
        }

        hideLoading();
        
        // Always show debug info about edges
        showEdgeDebugInfo(graphData);
        
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
            loading.innerHTML = '<p style="color:#ffa500;">Taking longer than expected. Check browser console.</p>';
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
    
    const debugBtn = document.getElementById('toggle-debug');
    if (debugBtn) {
        debugBtn.addEventListener('click', function() {
            const panel = document.getElementById('debug-panel');
            if (panel) {
                panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
            }
        });
    }
}

/**
 * Show edge debug information in the debug panel
 */
function showEdgeDebugInfo(graphData) {
    const countEl = document.getElementById('edge-count-debug');
    const listEl = document.getElementById('edge-list');
    
    if (!countEl || !listEl) return;
    
    const nodeCount = graphData.nodes ? graphData.nodes.length : 0;
    const edgeCount = graphData.edges ? graphData.edges.length : 0;
    
    countEl.innerHTML = 'Nodes: ' + nodeCount + ' | Edges: ' + edgeCount;
    
    if (!graphData.edges || graphData.edges.length === 0) {
        listEl.innerHTML = '<p style="color:#ff6b6b;">No edges received from backend!</p>' +
            '<p style="color:#888;">Check the terminal logs for details.</p>';
        return;
    }
    
    // Show first 50 edges
    let html = '<table style="width:100%;border-collapse:collapse;">';
    html += '<tr style="color:#888;border-bottom:1px solid #444;"><th style="text-align:left;padding:5px;">#</th><th style="text-align:left;padding:5px;">Source</th><th style="text-align:left;padding:5px;">Target</th><th style="text-align:left;padding:5px;">Field</th></tr>';
    
    const maxEdges = Math.min(graphData.edges.length, 50);
    for (let i = 0; i < maxEdges; i++) {
        const edge = graphData.edges[i];
        const sourceName = edge.source ? edge.source.split('.').pop() : 'null';
        const targetName = edge.target ? edge.target.split('.').pop() : 'null';
        html += '<tr style="border-bottom:1px solid #333;">';
        html += '<td style="padding:3px;color:#666;">' + (i + 1) + '</td>';
        html += '<td style="padding:3px;color:#88ff88;">' + sourceName + '</td>';
        html += '<td style="padding:3px;color:#ff8888;">' + targetName + '</td>';
        html += '<td style="padding:3px;color:#8888ff;">' + (edge.fieldName || '-') + '</td>';
        html += '</tr>';
    }
    html += '</table>';
    
    if (graphData.edges.length > 50) {
        html += '<p style="color:#888;margin-top:10px;">... and ' + (graphData.edges.length - 50) + ' more edges</p>';
    }
    
    listEl.innerHTML = html;
    
    // Also log to console
    console.log('=== EDGES DEBUG ===');
    console.log('Total edges:', graphData.edges.length);
    if (graphData.edges.length > 0) {
        console.log('First edge:', graphData.edges[0]);
        console.log('Sample node IDs:', graphData.nodes.slice(0, 5).map(n => n.id));
    }
}

window.addEventListener('beforeunload', function() {
    if (wsClient) wsClient.close();
});
