export interface CommandMessage {
  type: 'COMMAND';
  data: {
    parentId: string;
    section: string;
    elementId: string;
  };
  timestamp: number;
  message: 'updated_tree' | string;
}

export interface UpdatedTreeData {
  parentId: string;
  elementId: string;
  section: string;
}

class WebSocketService {
  private socket: WebSocket | null = null;
  private onUpdate: ((data: UpdatedTreeData) => void) | null = null;
  private shouldReconnect = true;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectDelay = 1000;
  private reconnectTimer: number | null = null;

  update(callback: (data: UpdatedTreeData) => void) {
    this.onUpdate = callback;
  }

  connect(): void {
    this.shouldReconnect = true;
    
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
      console.error('Missing access token for WebSocket connection');
      return;
    }

    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8762/websocket/web';
    const url = new URL(wsUrl);
    url.searchParams.append('token', accessToken);
    
    this.socket = new WebSocket(url.toString());

    this.socket.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.reconnectDelay = 1000;
    };

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as CommandMessage;
        this.processMessage(message);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    this.socket.onclose = () => {
      console.log('WebSocket disconnected');

      if (this.shouldReconnect && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.scheduleReconnect();
      }
    };

    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    this.reconnectAttempts++;
    const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1), 30000);

    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    this.reconnectTimer = window.setTimeout(() => {
      if (this.shouldReconnect) {
        this.connect();
      }
    }, delay);
  }

  private processMessage(message: CommandMessage): void {
    if (message.type !== 'COMMAND' || message.message !== 'updated_tree') {
      return;
    }

    if (!this.onUpdate) {
      console.warn('No data to update');
      return;
    }

    const { parentId, elementId, section } = message.data;
    
    this.onUpdate({
      parentId,
      elementId,
      section
    });
  }

  disconnect(): void {
    this.shouldReconnect = false;
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }

    this.reconnectAttempts = 0;
  }

  get connected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  get reconnectInfo(): { attempts: number; maxAttempts: number; isReconnecting: boolean } {
    return {
      attempts: this.reconnectAttempts,
      maxAttempts: this.maxReconnectAttempts,
      isReconnecting: this.reconnectTimer !== null
    };
  }
}

export const websocketService = new WebSocketService();

export function useWebSocketService() {
  return {
    connect: () => websocketService.connect(),
    disconnect: () => websocketService.disconnect(),
    update: (callback: (data: UpdatedTreeData) => void) => 
      websocketService.update(callback),
    connected: websocketService.connected,
    reconnectInfo: websocketService.reconnectInfo
  };
} 