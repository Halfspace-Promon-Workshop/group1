/**
 * WebSocket client for receiving graph data.
 */
class WebSocketClient {
    constructor(url, onGraphReceived) {
        this.url = url;
        this.onGraphReceived = onGraphReceived;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnects = 5;
    }

    connect() {
        console.log('Connecting to WebSocket:', this.url);

        try {
            this.ws = new WebSocket(this.url);

            this.ws.onopen = () => {
                console.log('WebSocket connected');
                this.reconnectAttempts = 0;
                this.updateStatus(true);
            };

            this.ws.onmessage = (event) => {
                console.log('WebSocket message received, length:', event.data.length);
                try {
                    const data = JSON.parse(event.data);
                    console.log('Parsed data type:', data.type);
                    
                    if (data.type === 'graph') {
                        this.onGraphReceived(data);
                    }
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.showError('WebSocket connection failed');
            };

            this.ws.onclose = (event) => {
                console.log('WebSocket closed:', event.code, event.reason);
                this.updateStatus(false);
                
                if (this.reconnectAttempts < this.maxReconnects) {
                    this.reconnectAttempts++;
                    console.log('Reconnecting in 2s... (attempt ' + this.reconnectAttempts + ')');
                    setTimeout(() => this.connect(), 2000);
                }
            };

        } catch (error) {
            console.error('Error creating WebSocket:', error);
            this.showError('Failed to create WebSocket connection');
        }
    }

    close() {
        if (this.ws) {
            this.ws.close();
        }
    }

    updateStatus(connected) {
        const el = document.getElementById('connection-status');
        if (el) {
            el.textContent = connected ? 'Connected' : 'Disconnected';
            el.className = connected ? 'connected' : 'disconnected';
        }
    }

    showError(message) {
        const loading = document.getElementById('loading');
        if (loading) {
            loading.innerHTML = '<p style="color:#ff6b6b;">' + message + '</p>';
        }
    }
}
