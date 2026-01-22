/**
 * 2D Graph visualization using HTML5 Canvas
 * Simple force-directed layout
 */
class GraphVisualizer {
    constructor(containerId) {
        console.log('=== GraphVisualizer Constructor ===');
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.canvas = null;
        this.ctx = null;
        this.nodes = [];
        this.edges = [];
        this.nodeMap = new Map();
        this.showLabels = true;
        
        // Interaction state
        this.draggedNode = null;
        this.hoveredNode = null;
        this.offsetX = 0;
        this.offsetY = 0;
        this.scale = 1;
        this.isPanning = false;
        this.lastMouseX = 0;
        this.lastMouseY = 0;
        
        this.init();
    }

    init() {
        console.log('Initializing 2D Canvas Graph...');
        
        if (!this.container) {
            console.error('Container not found:', this.containerId);
            return;
        }

        // Create canvas
        this.canvas = document.getElementById('graph-canvas');
        if (!this.canvas) {
            this.canvas = document.createElement('canvas');
            this.canvas.id = 'graph-canvas';
            this.container.appendChild(this.canvas);
        }
        
        this.ctx = this.canvas.getContext('2d');
        this.resizeCanvas();
        
        // Event listeners
        window.addEventListener('resize', () => this.resizeCanvas());
        this.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        this.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvas.addEventListener('mouseup', (e) => this.onMouseUp(e));
        this.canvas.addEventListener('wheel', (e) => this.onWheel(e));
        this.canvas.addEventListener('dblclick', (e) => this.onDoubleClick(e));
        
        // Start render loop
        this.animate();
        
        console.log('2D Graph Visualizer initialized');
    }

    resizeCanvas() {
        const rect = this.container.getBoundingClientRect();
        this.canvas.width = rect.width || window.innerWidth;
        this.canvas.height = rect.height || window.innerHeight - 80;
        console.log('Canvas resized:', this.canvas.width, 'x', this.canvas.height);
    }

    renderGraph(graphData) {
        console.log('=== renderGraph called ===');
        console.log('Nodes:', graphData?.nodes?.length || 0);
        console.log('Edges:', graphData?.edges?.length || 0);

        if (!graphData || !graphData.nodes) {
            console.warn('No graph data');
            return;
        }

        this.nodes = [];
        this.edges = [];
        this.nodeMap.clear();

        // Create nodes with initial positions
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;
        const radius = Math.min(centerX, centerY) * 0.7;

        graphData.nodes.forEach((nodeData, index) => {
            // Arrange in a circle initially
            const angle = (2 * Math.PI * index) / graphData.nodes.length;
            const node = {
                id: nodeData.id,
                name: nodeData.name || nodeData.id.split('.').pop(),
                fullName: nodeData.fullName || nodeData.id,
                x: centerX + radius * Math.cos(angle),
                y: centerY + radius * Math.sin(angle),
                vx: 0,
                vy: 0,
                radius: 20 + Math.min((nodeData.dependencyCount || 0) * 3, 15),
                color: this.getNodeColor(nodeData.dependencyCount || 0),
                dependencyCount: nodeData.dependencyCount || 0
            };
            this.nodes.push(node);
            this.nodeMap.set(nodeData.id, node);
        });

        // Create edges
        if (graphData.edges) {
            graphData.edges.forEach(edgeData => {
                const source = this.nodeMap.get(edgeData.source);
                const target = this.nodeMap.get(edgeData.target);
                if (source && target) {
                    this.edges.push({
                        source: source,
                        target: target,
                        fieldName: edgeData.fieldName
                    });
                }
            });
        }

        console.log('Created', this.nodes.length, 'nodes and', this.edges.length, 'edges');

        // Update stats
        this.updateStats(this.nodes.length, this.edges.length);

        // Run force simulation
        this.runForceSimulation();
    }

    getNodeColor(count) {
        if (count === 0) return '#e74c3c';  // Red
        if (count < 3) return '#9b59b6';    // Purple
        if (count < 6) return '#f39c12';    // Orange
        return '#27ae60';                    // Green
    }

    runForceSimulation() {
        const iterations = 100;
        const repulsion = 5000;
        const attraction = 0.01;
        const damping = 0.9;

        for (let iter = 0; iter < iterations; iter++) {
            // Repulsion between all nodes
            for (let i = 0; i < this.nodes.length; i++) {
                for (let j = i + 1; j < this.nodes.length; j++) {
                    const nodeA = this.nodes[i];
                    const nodeB = this.nodes[j];
                    
                    const dx = nodeB.x - nodeA.x;
                    const dy = nodeB.y - nodeA.y;
                    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                    
                    const force = repulsion / (dist * dist);
                    const fx = (dx / dist) * force;
                    const fy = (dy / dist) * force;
                    
                    nodeA.vx -= fx;
                    nodeA.vy -= fy;
                    nodeB.vx += fx;
                    nodeB.vy += fy;
                }
            }

            // Attraction along edges
            for (const edge of this.edges) {
                const dx = edge.target.x - edge.source.x;
                const dy = edge.target.y - edge.source.y;
                const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                
                const force = dist * attraction;
                const fx = (dx / dist) * force;
                const fy = (dy / dist) * force;
                
                edge.source.vx += fx;
                edge.source.vy += fy;
                edge.target.vx -= fx;
                edge.target.vy -= fy;
            }

            // Apply velocities and damping
            for (const node of this.nodes) {
                node.x += node.vx;
                node.y += node.vy;
                node.vx *= damping;
                node.vy *= damping;
                
                // Keep nodes in bounds
                const margin = 50;
                node.x = Math.max(margin, Math.min(this.canvas.width - margin, node.x));
                node.y = Math.max(margin, Math.min(this.canvas.height - margin, node.y));
            }
        }
    }

    animate() {
        this.draw();
        requestAnimationFrame(() => this.animate());
    }

    draw() {
        const ctx = this.ctx;
        if (!ctx) return;

        // Clear canvas
        ctx.fillStyle = '#0a0a1a';
        ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        ctx.save();
        ctx.translate(this.offsetX, this.offsetY);
        ctx.scale(this.scale, this.scale);

        // Draw edges first
        for (const edge of this.edges) {
            this.drawEdge(edge);
        }

        // Draw nodes on top
        for (const node of this.nodes) {
            this.drawNode(node);
        }

        ctx.restore();

        // Draw legend
        this.drawLegend();
    }

    drawEdge(edge) {
        const ctx = this.ctx;
        const source = edge.source;
        const target = edge.target;

        // Calculate direction
        const dx = target.x - source.x;
        const dy = target.y - source.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const nx = dx / dist;
        const ny = dy / dist;

        // Start and end points (adjusted for node radius)
        const startX = source.x + nx * source.radius;
        const startY = source.y + ny * source.radius;
        const endX = target.x - nx * target.radius;
        const endY = target.y - ny * target.radius;

        // Draw line
        ctx.beginPath();
        ctx.moveTo(startX, startY);
        ctx.lineTo(endX, endY);
        
        const isHighlighted = this.hoveredNode && 
            (edge.source === this.hoveredNode || edge.target === this.hoveredNode);
        
        ctx.strokeStyle = isHighlighted ? '#00ffff' : '#3498db';
        ctx.lineWidth = isHighlighted ? 3 : 2;
        ctx.stroke();

        // Draw arrow
        const arrowSize = 10;
        const arrowAngle = Math.atan2(endY - startY, endX - startX);
        
        ctx.beginPath();
        ctx.moveTo(endX, endY);
        ctx.lineTo(
            endX - arrowSize * Math.cos(arrowAngle - Math.PI / 6),
            endY - arrowSize * Math.sin(arrowAngle - Math.PI / 6)
        );
        ctx.lineTo(
            endX - arrowSize * Math.cos(arrowAngle + Math.PI / 6),
            endY - arrowSize * Math.sin(arrowAngle + Math.PI / 6)
        );
        ctx.closePath();
        ctx.fillStyle = isHighlighted ? '#00ffff' : '#3498db';
        ctx.fill();
    }

    drawNode(node) {
        const ctx = this.ctx;
        const isHovered = node === this.hoveredNode;

        // Draw glow for hovered node
        if (isHovered) {
            ctx.beginPath();
            ctx.arc(node.x, node.y, node.radius + 8, 0, Math.PI * 2);
            ctx.fillStyle = 'rgba(0, 255, 255, 0.3)';
            ctx.fill();
        }

        // Draw node circle
        ctx.beginPath();
        ctx.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
        
        // Gradient fill
        const gradient = ctx.createRadialGradient(
            node.x - node.radius/3, node.y - node.radius/3, 0,
            node.x, node.y, node.radius
        );
        gradient.addColorStop(0, this.lightenColor(node.color, 40));
        gradient.addColorStop(1, node.color);
        ctx.fillStyle = gradient;
        ctx.fill();

        // Border
        ctx.strokeStyle = isHovered ? '#00ffff' : '#ffffff';
        ctx.lineWidth = isHovered ? 3 : 2;
        ctx.stroke();

        // Draw label
        if (this.showLabels || isHovered) {
            ctx.font = 'bold 12px Arial';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            
            // Text background
            const textWidth = ctx.measureText(node.name).width;
            ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
            ctx.fillRect(
                node.x - textWidth/2 - 4,
                node.y + node.radius + 8,
                textWidth + 8,
                16
            );
            
            ctx.fillStyle = '#ffffff';
            ctx.fillText(node.name, node.x, node.y + node.radius + 16);
        }
    }

    lightenColor(color, percent) {
        const num = parseInt(color.replace('#', ''), 16);
        const amt = Math.round(2.55 * percent);
        const R = Math.min(255, (num >> 16) + amt);
        const G = Math.min(255, ((num >> 8) & 0x00FF) + amt);
        const B = Math.min(255, (num & 0x0000FF) + amt);
        return '#' + (0x1000000 + R * 0x10000 + G * 0x100 + B).toString(16).slice(1);
    }

    drawLegend() {
        const ctx = this.ctx;
        const x = 20;
        const y = this.canvas.height - 100;
        
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(x - 10, y - 10, 180, 90);
        
        ctx.font = 'bold 12px Arial';
        ctx.fillStyle = '#ffffff';
        ctx.textAlign = 'left';
        ctx.fillText('Dependency Count:', x, y);
        
        const colors = [
            { color: '#e74c3c', label: '0 deps' },
            { color: '#9b59b6', label: '1-2 deps' },
            { color: '#f39c12', label: '3-5 deps' },
            { color: '#27ae60', label: '6+ deps' }
        ];
        
        colors.forEach((item, i) => {
            const cy = y + 20 + i * 16;
            ctx.fillStyle = item.color;
            ctx.beginPath();
            ctx.arc(x + 8, cy, 6, 0, Math.PI * 2);
            ctx.fill();
            
            ctx.fillStyle = '#cccccc';
            ctx.font = '11px Arial';
            ctx.fillText(item.label, x + 20, cy + 4);
        });
    }

    // Mouse handlers
    onMouseDown(e) {
        const pos = this.getMousePos(e);
        const node = this.getNodeAtPos(pos.x, pos.y);
        
        if (node) {
            this.draggedNode = node;
        } else {
            this.isPanning = true;
            this.lastMouseX = e.clientX;
            this.lastMouseY = e.clientY;
        }
    }

    onMouseMove(e) {
        const pos = this.getMousePos(e);
        
        if (this.draggedNode) {
            this.draggedNode.x = pos.x;
            this.draggedNode.y = pos.y;
        } else if (this.isPanning) {
            this.offsetX += e.clientX - this.lastMouseX;
            this.offsetY += e.clientY - this.lastMouseY;
            this.lastMouseX = e.clientX;
            this.lastMouseY = e.clientY;
        } else {
            const node = this.getNodeAtPos(pos.x, pos.y);
            this.hoveredNode = node;
            this.canvas.style.cursor = node ? 'pointer' : 'default';
            
            if (node) {
                this.showHoverInfo(node);
            } else {
                this.clearHoverInfo();
            }
        }
    }

    onMouseUp(e) {
        this.draggedNode = null;
        this.isPanning = false;
    }

    onWheel(e) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        this.scale *= delta;
        this.scale = Math.max(0.2, Math.min(3, this.scale));
    }

    onDoubleClick(e) {
        // Reset view
        this.resetCamera();
    }

    getMousePos(e) {
        const rect = this.canvas.getBoundingClientRect();
        return {
            x: (e.clientX - rect.left - this.offsetX) / this.scale,
            y: (e.clientY - rect.top - this.offsetY) / this.scale
        };
    }

    getNodeAtPos(x, y) {
        for (let i = this.nodes.length - 1; i >= 0; i--) {
            const node = this.nodes[i];
            const dx = x - node.x;
            const dy = y - node.y;
            if (dx * dx + dy * dy <= node.radius * node.radius) {
                return node;
            }
        }
        return null;
    }

    resetCamera() {
        this.offsetX = 0;
        this.offsetY = 0;
        this.scale = 1;
    }

    toggleLabels() {
        this.showLabels = !this.showLabels;
    }

    showHoverInfo(node) {
        const el = document.getElementById('hover-info');
        if (el) {
            el.innerHTML = '<strong>' + node.name + '</strong><br>' +
                node.fullName + '<br>Dependencies: ' + node.dependencyCount;
        }
    }

    clearHoverInfo() {
        const el = document.getElementById('hover-info');
        if (el) el.textContent = 'Hover over a node to see details';
    }

    updateStats(nodeCount, edgeCount) {
        const nodeEl = document.getElementById('node-count');
        const edgeEl = document.getElementById('edge-count');
        if (nodeEl) nodeEl.textContent = 'Nodes: ' + nodeCount;
        if (edgeEl) edgeEl.textContent = 'Edges: ' + edgeCount;
    }
}

window.GraphVisualizer = GraphVisualizer;
