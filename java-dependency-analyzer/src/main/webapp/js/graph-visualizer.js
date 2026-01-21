/**
 * 3D Graph visualization using Three.js
 */
class GraphVisualizer {
    constructor(containerId) {
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

        this.init();
    }

    /**
     * Initialize the Three.js scene.
     */
    init() {
        // Create scene
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x1e1e2e);

        // Create camera
        const aspect = this.container.clientWidth / this.container.clientHeight;
        this.camera = new THREE.PerspectiveCamera(75, aspect, 0.1, 10000);
        this.camera.position.set(0, 0, 500);

        // Create renderer
        this.renderer = new THREE.WebGLRenderer({
            canvas: document.getElementById('graph-canvas'),
            antialias: true
        });
        this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
        this.renderer.setPixelRatio(window.devicePixelRatio);

        // Add orbit controls
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;
        this.controls.rotateSpeed = 0.5;
        this.controls.zoomSpeed = 1.2;

        // Add lights
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
        this.scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(100, 100, 100);
        this.scene.add(directionalLight);

        // Add event listeners
        window.addEventListener('resize', () => this.onWindowResize());
        this.renderer.domElement.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.renderer.domElement.addEventListener('click', (e) => this.onMouseClick(e));

        // Start animation loop
        this.animate();

        console.log('Graph visualizer initialized');
    }

    /**
     * Render the graph data.
     */
    renderGraph(graphData) {
        console.log('Rendering graph:', graphData);
        this.graphData = graphData;

        // Clear existing graph
        this.clearGraph();

        if (!graphData.nodes || graphData.nodes.length === 0) {
            console.warn('No nodes to render');
            return;
        }

        // Create nodes
        this.createNodes(graphData.nodes);

        // Create edges
        if (graphData.edges) {
            this.createEdges(graphData.edges);
        }

        // Update stats
        this.updateStats(graphData.nodes.length, graphData.edges?.length || 0);

        // Center camera on graph
        this.centerCamera();
    }

    /**
     * Create node spheres.
     */
    createNodes(nodes) {
        nodes.forEach(nodeData => {
            // Calculate node size based on dependency count
            const baseSize = 5;
            const sizeMultiplier = 1 + (nodeData.dependencyCount || 0) * 0.1;
            const size = baseSize * Math.min(sizeMultiplier, 3);

            // Create sphere geometry
            const geometry = new THREE.SphereGeometry(size, 32, 32);

            // Color based on dependency count
            const color = this.getNodeColor(nodeData.dependencyCount || 0);
            const material = new THREE.MeshPhongMaterial({
                color: color,
                emissive: color,
                emissiveIntensity: 0.2,
                shininess: 100
            });

            const sphere = new THREE.Mesh(geometry, material);
            sphere.position.set(nodeData.x, nodeData.y, nodeData.z);

            // Store node data
            sphere.userData = {
                id: nodeData.id,
                name: nodeData.name,
                fullName: nodeData.fullName,
                dependencyCount: nodeData.dependencyCount
            };

            this.scene.add(sphere);
            this.nodes.set(nodeData.id, sphere);

            // Create label if enabled
            if (this.showLabels) {
                this.createLabel(nodeData.name, sphere.position);
            }
        });

        console.log(`Created ${nodes.length} node spheres`);
    }

    /**
     * Create edges between nodes.
     */
    createEdges(edges) {
        edges.forEach(edgeData => {
            const sourceNode = this.nodes.get(edgeData.source);
            const targetNode = this.nodes.get(edgeData.target);

            if (sourceNode && targetNode) {
                const points = [
                    sourceNode.position.clone(),
                    targetNode.position.clone()
                ];

                const geometry = new THREE.BufferGeometry().setFromPoints(points);
                const material = new THREE.LineBasicMaterial({
                    color: 0x6677ff,
                    transparent: true,
                    opacity: 0.3,
                    linewidth: 1
                });

                const line = new THREE.Line(geometry, material);
                line.userData = {
                    source: edgeData.source,
                    target: edgeData.target,
                    fieldName: edgeData.fieldName
                };

                this.scene.add(line);
                this.edges.push(line);
            }
        });

        console.log(`Created ${this.edges.length} edges`);
    }

    /**
     * Create a text label for a node.
     */
    createLabel(text, position) {
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        canvas.width = 256;
        canvas.height = 64;

        context.fillStyle = 'rgba(0, 0, 0, 0.7)';
        context.fillRect(0, 0, canvas.width, canvas.height);

        context.font = 'Bold 20px Arial';
        context.fillStyle = 'white';
        context.textAlign = 'center';
        context.textBaseline = 'middle';
        context.fillText(text, canvas.width / 2, canvas.height / 2);

        const texture = new THREE.CanvasTexture(canvas);
        const material = new THREE.SpriteMaterial({
            map: texture,
            transparent: true,
            opacity: 0.8
        });

        const sprite = new THREE.Sprite(material);
        sprite.position.set(position.x, position.y + 15, position.z);
        sprite.scale.set(50, 12.5, 1);

        this.scene.add(sprite);
    }

    /**
     * Get color for a node based on dependency count.
     */
    getNodeColor(dependencyCount) {
        if (dependencyCount === 0) {
            return 0x4444ff; // Blue for leaf nodes
        } else if (dependencyCount < 3) {
            return 0x44ff44; // Green for low dependencies
        } else if (dependencyCount < 6) {
            return 0xffff44; // Yellow for medium dependencies
        } else {
            return 0xff4444; // Red for high dependencies
        }
    }

    /**
     * Clear the graph.
     */
    clearGraph() {
        // Remove nodes
        this.nodes.forEach(node => {
            this.scene.remove(node);
            node.geometry.dispose();
            node.material.dispose();
        });
        this.nodes.clear();

        // Remove edges
        this.edges.forEach(edge => {
            this.scene.remove(edge);
            edge.geometry.dispose();
            edge.material.dispose();
        });
        this.edges = [];

        // Remove sprites (labels)
        const sprites = this.scene.children.filter(child => child instanceof THREE.Sprite);
        sprites.forEach(sprite => {
            this.scene.remove(sprite);
            sprite.material.map.dispose();
            sprite.material.dispose();
        });
    }

    /**
     * Center the camera on the graph.
     */
    centerCamera() {
        if (this.nodes.size === 0) return;

        const box = new THREE.Box3();
        this.nodes.forEach(node => {
            box.expandByObject(node);
        });

        const center = box.getCenter(new THREE.Vector3());
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z);
        const fov = this.camera.fov * (Math.PI / 180);
        const cameraZ = Math.abs(maxDim / 2 / Math.tan(fov / 2)) * 1.5;

        this.camera.position.set(center.x, center.y, center.z + cameraZ);
        this.controls.target.copy(center);
        this.controls.update();
    }

    /**
     * Reset camera to default position.
     */
    resetCamera() {
        this.centerCamera();
    }

    /**
     * Toggle label visibility.
     */
    toggleLabels() {
        this.showLabels = !this.showLabels;
        if (this.graphData) {
            this.renderGraph(this.graphData);
        }
    }

    /**
     * Handle mouse move for hover effects.
     */
    onMouseMove(event) {
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
     * Handle mouse click for selection.
     */
    onMouseClick(event) {
        if (this.hoveredNode) {
            this.selectNode(this.hoveredNode);
        }
    }

    /**
     * Select a node and highlight its connections.
     */
    selectNode(node) {
        this.selectedNode = node;
        this.showSelectedInfo(node.userData);

        // Highlight connected edges
        this.edges.forEach(edge => {
            if (edge.userData.source === node.userData.id || edge.userData.target === node.userData.id) {
                edge.material.opacity = 0.8;
                edge.material.color.setHex(0xffff00);
            } else {
                edge.material.opacity = 0.1;
                edge.material.color.setHex(0x6677ff);
            }
        });
    }

    /**
     * Show hover information.
     */
    showHoverInfo(nodeData) {
        const hoverInfo = document.getElementById('hover-info');
        if (hoverInfo) {
            hoverInfo.innerHTML = `
                <strong>${nodeData.name}</strong><br>
                ${nodeData.fullName}<br>
                Dependencies: ${nodeData.dependencyCount}
            `;
        }
    }

    /**
     * Clear hover information.
     */
    clearHoverInfo() {
        const hoverInfo = document.getElementById('hover-info');
        if (hoverInfo) {
            hoverInfo.textContent = 'Hover over a node to see details';
        }
    }

    /**
     * Show selected node information.
     */
    showSelectedInfo(nodeData) {
        const selectedInfo = document.getElementById('selected-info');
        if (selectedInfo) {
            selectedInfo.innerHTML = `
                <strong>Selected:</strong> ${nodeData.name}<br>
                <strong>Full Name:</strong> ${nodeData.fullName}<br>
                <strong>Dependencies:</strong> ${nodeData.dependencyCount}
            `;
        }
    }

    /**
     * Update statistics display.
     */
    updateStats(nodeCount, edgeCount) {
        const nodeCountEl = document.getElementById('node-count');
        const edgeCountEl = document.getElementById('edge-count');

        if (nodeCountEl) nodeCountEl.textContent = `Nodes: ${nodeCount}`;
        if (edgeCountEl) edgeCountEl.textContent = `Edges: ${edgeCount}`;
    }

    /**
     * Handle window resize.
     */
    onWindowResize() {
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;

        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();

        this.renderer.setSize(width, height);
    }

    /**
     * Animation loop.
     */
    animate() {
        requestAnimationFrame(() => this.animate());

        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }
}
