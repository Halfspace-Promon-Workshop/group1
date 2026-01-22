/**
 * 3D Graph visualization using Three.js
 * Simplified and robust implementation
 */
class GraphVisualizer {
    constructor(containerId) {
        console.log('=== GraphVisualizer Constructor ===');
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.controls = null;
        this.nodes = new Map();
        this.edges = [];
        this.graphData = null;
        this.raycaster = new THREE.Raycaster();
        this.mouse = new THREE.Vector2();
        this.selectedNode = null;
        this.hoveredNode = null;
        this.showLabels = true;
        this.isInitialized = false;

        this.init();
    }

    /**
     * Initialize the Three.js scene.
     */
    init() {
        console.log('Initializing Three.js...');
        
        try {
            // Verify Three.js is loaded
            if (typeof THREE === 'undefined') {
                throw new Error('THREE.js is not loaded');
            }
            console.log('THREE.js version:', THREE.REVISION);

            // Verify container
            if (!this.container) {
                throw new Error('Container not found: ' + this.containerId);
            }

            // Get container size
            let width = this.container.clientWidth || window.innerWidth;
            let height = this.container.clientHeight || window.innerHeight - 100;
            if (height < 400) height = 400;
            console.log('Container size:', width, 'x', height);

            // Create scene
            this.scene = new THREE.Scene();
            this.scene.background = new THREE.Color(0x1a1a2e);

            // Create camera
            this.camera = new THREE.PerspectiveCamera(60, width / height, 1, 10000);
            this.camera.position.set(0, 0, 800);

            // Get or create canvas
            let canvas = document.getElementById('graph-canvas');
            if (!canvas) {
                canvas = document.createElement('canvas');
                canvas.id = 'graph-canvas';
                this.container.appendChild(canvas);
            }

            // Create renderer
            this.renderer = new THREE.WebGLRenderer({
                canvas: canvas,
                antialias: true,
                alpha: false
            });
            this.renderer.setSize(width, height);
            this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
            console.log('Renderer created');

            // Create orbit controls - try multiple ways to access OrbitControls
            let OrbitControlsClass = null;
            
            // Try THREE.OrbitControls first
            if (typeof THREE !== 'undefined' && typeof THREE.OrbitControls !== 'undefined') {
                OrbitControlsClass = THREE.OrbitControls;
            }
            // Try global OrbitControls
            else if (typeof OrbitControls !== 'undefined') {
                OrbitControlsClass = OrbitControls;
                // Attach to THREE namespace for consistency
                if (typeof THREE !== 'undefined') {
                    THREE.OrbitControls = OrbitControls;
                }
            }
            // Try accessing from THREE namespace if it was loaded differently
            else if (typeof THREE !== 'undefined' && THREE.controls && THREE.controls.OrbitControls) {
                OrbitControlsClass = THREE.controls.OrbitControls;
            }
            
            if (OrbitControlsClass) {
                this.controls = new OrbitControlsClass(this.camera, this.renderer.domElement);
                this.controls.enableDamping = true;
                this.controls.dampingFactor = 0.1;
                console.log('OrbitControls created successfully');
            } else {
                console.error('OrbitControls not available. THREE:', typeof THREE, 'OrbitControls:', typeof OrbitControls);
                console.error('Camera controls will not work. Please check that OrbitControls script is loaded.');
            }

            // Add lights
            const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
            this.scene.add(ambientLight);

            const light1 = new THREE.DirectionalLight(0xffffff, 0.8);
            light1.position.set(100, 100, 100);
            this.scene.add(light1);

            const light2 = new THREE.DirectionalLight(0xffffff, 0.3);
            light2.position.set(-100, -100, -100);
            this.scene.add(light2);

            // Add event listeners
            window.addEventListener('resize', () => this.onWindowResize());
            this.renderer.domElement.addEventListener('mousemove', (e) => this.onMouseMove(e));
            this.renderer.domElement.addEventListener('click', (e) => this.onMouseClick(e));

            // Start animation
            this.isInitialized = true;
            this.animate();

            // Test render
            this.renderer.render(this.scene, this.camera);
            console.log('GraphVisualizer initialized successfully');

        } catch (error) {
            console.error('Error initializing GraphVisualizer:', error);
            this.showError(error.message);
        }
    }

    /**
     * Show error message
     */
    showError(message) {
        if (this.container) {
            const div = document.createElement('div');
            div.style.cssText = 'position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:#ff6b6b;text-align:center;background:rgba(0,0,0,0.8);padding:20px;border-radius:10px;';
            div.innerHTML = '<h3>Visualization Error</h3><p>' + message + '</p>';
            this.container.appendChild(div);
        }
    }

    /**
     * Render the graph data.
     */
    renderGraph(graphData) {
        console.log('=== renderGraph called ===');
        console.log('Graph data:', graphData);
        console.log('Nodes count:', graphData?.nodes?.length);
        console.log('Edges count:', graphData?.edges?.length);

        if (!this.isInitialized || !this.scene) {
            console.error('GraphVisualizer not initialized');
            return;
        }

        this.graphData = graphData;
        this.clearGraph();

        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
            console.warn('No nodes to render');
            return;
        }

        // Create nodes
        console.log('Creating nodes...');
        this.createNodes(graphData.nodes);
        console.log('Nodes created:', this.nodes.size);

        // Create edges
        if (graphData.edges && graphData.edges.length > 0) {
            console.log('Creating edges...');
            this.createEdges(graphData.edges);
            console.log('Edges created:', this.edges.length);
        } else {
            console.warn('No edges in graph data');
        }

        // Update stats
        this.updateStats(this.nodes.size, this.edges.length);

        // Center camera
        this.centerCamera();

        console.log('=== Graph rendering complete ===');
    }

    /**
     * Create node spheres.
     */
    createNodes(nodes) {
        const gridSize = Math.ceil(Math.sqrt(nodes.length));
        const spacing = 80;

        nodes.forEach((nodeData, index) => {
            // Get or calculate position
            let x = nodeData.x;
            let y = nodeData.y;
            let z = nodeData.z;

            // If no valid position, arrange in grid
            // Use strict equality to check for undefined/null, not falsy values (0 is valid)
            if (x === undefined || x === null || y === undefined || y === null || z === undefined || z === null) {
                const row = Math.floor(index / gridSize);
                const col = index % gridSize;
                x = (col - gridSize / 2) * spacing;
                y = (row - gridSize / 2) * spacing;
                z = (Math.random() - 0.5) * spacing;
            }

            // Node size based on dependencies
            const size = 8 + Math.min((nodeData.dependencyCount || 0) * 2, 20);

            // Node color based on dependencies
            const color = this.getNodeColor(nodeData.dependencyCount || 0);

            // Create sphere
            const geometry = new THREE.SphereGeometry(size, 16, 16);
            const material = new THREE.MeshPhongMaterial({
                color: color,
                emissive: color,
                emissiveIntensity: 0.2,
                shininess: 50
            });

            const sphere = new THREE.Mesh(geometry, material);
            sphere.position.set(x, y, z);
            sphere.userData = {
                id: nodeData.id,
                name: nodeData.name || nodeData.id,
                fullName: nodeData.fullName || nodeData.id,
                dependencyCount: nodeData.dependencyCount || 0
            };

            this.scene.add(sphere);
            this.nodes.set(nodeData.id, sphere);

            // Create label
            if (this.showLabels && index < 50) {
                this.createLabel(nodeData.name || nodeData.id, sphere.position);
            }
        });
    }

    /**
     * Create edges between nodes.
     */
    createEdges(edgesData) {
        let created = 0;
        let failed = 0;

        edgesData.forEach((edge, i) => {
            const sourceNode = this.nodes.get(edge.source);
            const targetNode = this.nodes.get(edge.target);

            if (!sourceNode) {
                if (i < 5) console.warn('Edge source not found:', edge.source);
                failed++;
                return;
            }
            if (!targetNode) {
                if (i < 5) console.warn('Edge target not found:', edge.target);
                failed++;
                return;
            }

            // Create line from source to target
            const points = [
                sourceNode.position.clone(),
                targetNode.position.clone()
            ];

            const geometry = new THREE.BufferGeometry().setFromPoints(points);
            const material = new THREE.LineBasicMaterial({
                color: 0x4488ff,
                transparent: true,
                opacity: 0.6
            });

            const line = new THREE.Line(geometry, material);
            line.userData = {
                source: edge.source,
                target: edge.target,
                fieldName: edge.fieldName
            };

            this.scene.add(line);
            this.edges.push(line);
            created++;
        });

        console.log('Edges: created=' + created + ', failed=' + failed);
        
        // Debug: log node IDs if edges failed
        if (failed > 0 && created === 0) {
            console.log('Available node IDs (first 10):');
            const ids = Array.from(this.nodes.keys()).slice(0, 10);
            ids.forEach(id => console.log('  - ' + id));
            
            console.log('Edge source IDs (first 10):');
            edgesData.slice(0, 10).forEach(e => console.log('  - ' + e.source));
        }
    }

    /**
     * Create label for node
     */
    createLabel(text, position) {
        const canvas = document.createElement('canvas');
        canvas.width = 256;
        canvas.height = 64;
        const ctx = canvas.getContext('2d');
        
        ctx.fillStyle = 'rgba(0,0,0,0.7)';
        ctx.fillRect(0, 0, 256, 64);
        ctx.font = '18px Arial';
        ctx.fillStyle = 'white';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        
        // Truncate text if too long
        let displayText = text;
        if (text.length > 20) {
            displayText = text.substring(0, 18) + '...';
        }
        ctx.fillText(displayText, 128, 32);

        const texture = new THREE.CanvasTexture(canvas);
        const material = new THREE.SpriteMaterial({ map: texture, transparent: true, opacity: 0.8 });
        const sprite = new THREE.Sprite(material);
        sprite.position.set(position.x, position.y + 20, position.z);
        sprite.scale.set(40, 10, 1);
        this.scene.add(sprite);
    }

    /**
     * Get color based on dependency count
     */
    getNodeColor(count) {
        if (count === 0) return 0x3366cc;
        if (count < 3) return 0x44aa44;
        if (count < 6) return 0xcccc44;
        return 0xcc4444;
    }

    /**
     * Clear the graph
     */
    clearGraph() {
        // Remove nodes
        this.nodes.forEach(node => {
            this.scene.remove(node);
            if (node.geometry) node.geometry.dispose();
            if (node.material) node.material.dispose();
        });
        this.nodes.clear();

        // Remove edges
        this.edges.forEach(edge => {
            this.scene.remove(edge);
            if (edge.geometry) edge.geometry.dispose();
            if (edge.material) edge.material.dispose();
        });
        this.edges = [];

        // Remove sprites
        const sprites = this.scene.children.filter(c => c instanceof THREE.Sprite);
        sprites.forEach(sprite => {
            this.scene.remove(sprite);
            if (sprite.material && sprite.material.map) sprite.material.map.dispose();
            if (sprite.material) sprite.material.dispose();
        });
    }

    /**
     * Center camera on graph
     */
    centerCamera() {
        if (this.nodes.size === 0) return;

        const box = new THREE.Box3();
        this.nodes.forEach(node => box.expandByObject(node));

        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z, 200);

        const distance = maxDim * 1.5;
        this.camera.position.set(center.x, center.y, center.z + distance);
        
        if (this.controls) {
            this.controls.target.copy(center);
            this.controls.update();
        }

        console.log('Camera centered at distance:', distance);
    }

    /**
     * Reset camera
     */
    resetCamera() {
        this.centerCamera();
    }

    /**
     * Toggle labels
     */
    toggleLabels() {
        this.showLabels = !this.showLabels;
        if (this.graphData) {
            this.renderGraph(this.graphData);
        }
    }

    /**
     * Window resize handler
     */
    onWindowResize() {
        if (!this.camera || !this.renderer || !this.container) return;
        
        const width = this.container.clientWidth || window.innerWidth;
        const height = this.container.clientHeight || window.innerHeight - 100;
        
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height);
    }

    /**
     * Mouse move handler
     */
    onMouseMove(event) {
        if (!this.renderer) return;
        
        const rect = this.renderer.domElement.getBoundingClientRect();
        this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
        this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

        this.raycaster.setFromCamera(this.mouse, this.camera);
        const intersects = this.raycaster.intersectObjects([...this.nodes.values()]);

        if (intersects.length > 0) {
            const node = intersects[0].object;
            if (this.hoveredNode !== node) {
                this.hoveredNode = node;
                this.showHoverInfo(node.userData);
                this.renderer.domElement.style.cursor = 'pointer';
            }
        } else {
            if (this.hoveredNode) {
                this.hoveredNode = null;
                this.clearHoverInfo();
                this.renderer.domElement.style.cursor = 'default';
            }
        }
    }

    /**
     * Mouse click handler
     */
    onMouseClick(event) {
        if (this.hoveredNode) {
            this.selectNode(this.hoveredNode);
        }
    }

    /**
     * Select a node
     */
    selectNode(node) {
        this.selectedNode = node;
        this.showSelectedInfo(node.userData);

        // Highlight connected edges
        this.edges.forEach(edge => {
            const connected = edge.userData.source === node.userData.id || 
                              edge.userData.target === node.userData.id;
            edge.material.opacity = connected ? 1.0 : 0.2;
            edge.material.color.setHex(connected ? 0xffff00 : 0x4488ff);
        });
    }

    /**
     * Show hover info
     */
    showHoverInfo(data) {
        const el = document.getElementById('hover-info');
        if (el) {
            el.innerHTML = '<strong>' + data.name + '</strong><br>' + 
                           data.fullName + '<br>Dependencies: ' + data.dependencyCount;
        }
    }

    /**
     * Clear hover info
     */
    clearHoverInfo() {
        const el = document.getElementById('hover-info');
        if (el) el.textContent = 'Hover over a node to see details';
    }

    /**
     * Show selected info
     */
    showSelectedInfo(data) {
        const el = document.getElementById('selected-info');
        if (el) {
            el.innerHTML = '<strong>Selected:</strong> ' + data.name + '<br>' +
                           '<strong>Full:</strong> ' + data.fullName + '<br>' +
                           '<strong>Dependencies:</strong> ' + data.dependencyCount;
        }
    }

    /**
     * Update stats display
     */
    updateStats(nodeCount, edgeCount) {
        const nodeEl = document.getElementById('node-count');
        const edgeEl = document.getElementById('edge-count');
        if (nodeEl) nodeEl.textContent = 'Nodes: ' + nodeCount;
        if (edgeEl) edgeEl.textContent = 'Edges: ' + edgeCount;
    }

    /**
     * Animation loop
     */
    animate() {
        if (!this.isInitialized) return;
        
        requestAnimationFrame(() => this.animate());
        
        if (this.controls) this.controls.update();
        if (this.renderer && this.scene && this.camera) {
            this.renderer.render(this.scene, this.camera);
        }
    }
}

// Export for debugging
window.GraphVisualizer = GraphVisualizer;
