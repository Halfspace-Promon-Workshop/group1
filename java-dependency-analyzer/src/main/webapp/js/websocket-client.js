/**
 * WebSocket client for receiving graph data from the server.
 */
class WebSocketClient {
    constructor(url, onGraphReceived) {
        this.url = url;
        this.onGraphReceived = onGraphReceived;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 2000;
        this.isIntentionallyClosed = false;
    }

    /**
     * Connect to the WebSocket server.
     */
    connect() {
        console.log(`Attempting to connect to WebSocket at ${this.url}`);
        this.isIntentionallyClosed = false;

        try {
            this.ws = new WebSocket(this.url);
            console.log('WebSocket object created, waiting for connection...');

            this.ws.onopen = () => {
                console.log('WebSocket connected');
                this.reconnectAttempts = 0;
                this.updateConnectionStatus(true);
                
                // Hide loading screen after a brief delay if no data received
                // This handles the case where the server has no data to send
                setTimeout(() => {
                    const loadingElement = document.getElementById('loading');
                    if (loadingElement && !loadingElement.classList.contains('hidden')) {
                        console.log('No graph data received, but connection established. Hiding loading screen.');
                        loadingElement.innerHTML = `
                            <div class="spinner" style="border-top-color: #ffa500;"></div>
                            <p style="color: #ffa500;">Connected, but no data available. The workspace may be empty or still analyzing.</p>
                        `;
                        // Hide after showing the message briefly
                        setTimeout(() => {
                            loadingElement.classList.add('hidden');
                        }, 2000);
                    }
                }, 3000);
            };

            this.ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    console.log('Received graph data:', {
                        nodes: data.nodes?.length || 0,
                        edges: data.edges?.length || 0
                    });

                    if (data.type === 'graph') {
                        this.onGraphReceived(data);
                    }
                } catch (error) {
                    console.error('Error parsing message:', error);
                }
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                console.error('Failed to connect to:', this.url);
                this.showError(`Failed to connect to WebSocket at ${this.url}. Check that the server is running.`);
            };

            this.ws.onclose = (event) => {
                console.log('WebSocket closed:', event.code, event.reason);
                this.updateConnectionStatus(false);

                // Attempt to reconnect if not intentionally closed
                if (!this.isIntentionallyClosed) {
                    this.attemptReconnect();
                }
            };

        } catch (error) {
            console.error('Error creating WebSocket:', error);
            this.updateConnectionStatus(false);
            this.attemptReconnect();
        }
    }

    /**
     * Attempt to reconnect to the server.
     */
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = this.reconnectDelay * this.reconnectAttempts;

            console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

            setTimeout(() => {
                if (!this.isIntentionallyClosed) {
                    this.connect();
                }
            }, delay);
        } else {
            console.error('Max reconnection attempts reached');
            this.showError('Connection lost. Please refresh the page.');
        }
    }

    /**
     * Send a message to the server.
     */
    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            console.warn('WebSocket is not open. Message not sent:', message);
        }
    }

    /**
     * Close the WebSocket connection.
     */
    close() {
        this.isIntentionallyClosed = true;
        if (this.ws) {
            this.ws.close();
        }
    }

    /**
     * Update the connection status indicator in the UI.
     */
    updateConnectionStatus(connected) {
        const statusElement = document.getElementById('connection-status');
        if (statusElement) {
            if (connected) {
                statusElement.textContent = 'Connected';
                statusElement.className = 'connected';
            } else {
                statusElement.textContent = 'Disconnected';
                statusElement.className = 'disconnected';
            }
        }
    }

    /**
     * Show an error message to the user.
     */
    showError(message) {
        const loadingElement = document.getElementById('loading');
        if (loadingElement) {
            loadingElement.innerHTML = `
                <div class="spinner" style="border-top-color: #ff6b6b;"></div>
                <p style="color: #ff6b6b;">${message}</p>
            `;
            loadingElement.classList.remove('hidden');
        }
    }

    /**
     * Check if the WebSocket is connected.
     */
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}
