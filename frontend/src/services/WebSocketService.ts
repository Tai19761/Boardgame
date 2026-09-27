import { Client, type StompSubscription } from '@stomp/stompjs';
import { type GameStateResponse, type MoveRequest, useGameStore } from '../store/useGameStore';

export class WebSocketService {
  private static instance: WebSocketService;
  private client: Client;
  private matchSubscription: StompSubscription | null = null;
  private disconnectTimer: number | null = null;

  private constructor() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws-boardgame',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        useGameStore.getState().setIsConnected(true);
        useGameStore.getState().clearError();

        // 1. Subscribe /user/queue/errors để hứng lỗi cá nhân
        this.client.subscribe('/user/queue/errors', (message) => {
          try {
            const data = JSON.parse(message.body);
            const msg = data.message || JSON.stringify(data);
            useGameStore.getState().setErrorMessage(msg);
          } catch {
            useGameStore.getState().setErrorMessage(message.body);
          }
        });

        // 2. Subscribe /user/queue/room/created khi Host tạo phòng xong
        this.client.subscribe('/user/queue/room/created', (message) => {
          try {
            const data = JSON.parse(message.body);
            const roomId = data.roomId;
            if (roomId) {
              // BẮT BUỘC subscribe /topic/match/{roomId} NGAY LẬP TỨC để lắng nghe cập nhật bàn cờ
              this.subscribeToMatch(roomId);
              // Lưu myPlayer = 'PLAYER_1' và roomId vào Zustand
              useGameStore.getState().setMyPlayer('PLAYER_1');
              useGameStore.getState().setRoomId(roomId);
            }
          } catch (err) {
            console.error('Lỗi khi xử lý room/created:', err);
            useGameStore.getState().setErrorMessage('Không thể xử lý phản hồi tạo phòng');
          }
        });

        // 3. Khôi phục Subscription bàn cờ khi Reconnect nếu đã có roomId
        const currentRoomId = useGameStore.getState().roomId;
        if (currentRoomId) {
          this.subscribeToMatch(currentRoomId);
        }
      },

      onStompError: (frame) => {
        const errorMsg = frame.headers['message'] || 'Lỗi giao thức STOMP';
        useGameStore.getState().setErrorMessage(`STOMP Error: ${errorMsg}`);
      },

      onWebSocketError: (event) => {
        console.error('WebSocket error:', event);
        useGameStore.getState().setErrorMessage('Lỗi kết nối WebSocket máy chủ');
      },

      onDisconnect: () => {
        useGameStore.getState().setIsConnected(false);
      },

      onWebSocketClose: () => {
        useGameStore.getState().setIsConnected(false);
      },
    });
  }

  public static getInstance(): WebSocketService {
    if (!WebSocketService.instance) {
      WebSocketService.instance = new WebSocketService();
    }
    return WebSocketService.instance;
  }

  public connect(): void {
    if (this.disconnectTimer !== null) {
      window.clearTimeout(this.disconnectTimer);
      this.disconnectTimer = null;
    }

    if (this.client.active) {
      return;
    }

    this.client.activate();
  }

  public disconnect(): void {
    if (this.disconnectTimer !== null) {
      window.clearTimeout(this.disconnectTimer);
    }

    // Trì hoãn nhẹ 100ms để an toàn với chu kỳ mount -> unmount -> mount của React StrictMode
    this.disconnectTimer = window.setTimeout(() => {
      if (this.client.active) {
        this.cleanupMatchSubscription();
        this.client.deactivate();
        useGameStore.getState().setIsConnected(false);
      }
      this.disconnectTimer = null;
    }, 100);
  }

  public subscribeToMatch(roomId: string): void {
    this.cleanupMatchSubscription();

    this.matchSubscription = this.client.subscribe(
      `/topic/match/${roomId}`,
      (message) => {
        try {
          const state: GameStateResponse = JSON.parse(message.body);
          useGameStore.getState().setGameState(state);
        } catch (err) {
          console.error('Lỗi parse trạng thái trận đấu:', err);
        }
      }
    );
  }

  private cleanupMatchSubscription(): void {
    if (this.matchSubscription) {
      this.matchSubscription.unsubscribe();
      this.matchSubscription = null;
    }
  }

  public createRoom(gameType: string = 'CARO'): void {
    if (!this.client.connected) {
      useGameStore.getState().setErrorMessage('Chưa kết nối tới máy chủ WebSocket');
      return;
    }

    this.client.publish({
      destination: '/app/room/create',
      body: JSON.stringify({ gameType }),
    });
  }

  public joinRoom(roomId: string): void {
    if (!this.client.connected) {
      useGameStore.getState().setErrorMessage('Chưa kết nối tới máy chủ WebSocket');
      return;
    }

    const cleanRoomId = roomId.trim();
    if (!cleanRoomId) {
      useGameStore.getState().setErrorMessage('Vui lòng nhập mã phòng');
      return;
    }

    // 1. BẮT BUỘC subscribe /topic/match/{roomId} TRƯỚC
    this.subscribeToMatch(cleanRoomId);

    // 2. Sau đó mới gửi message tới /app/room/join
    this.client.publish({
      destination: '/app/room/join',
      body: JSON.stringify({ roomId: cleanRoomId }),
    });

    // 3. Cập nhật myPlayer = 'PLAYER_2' và roomId vào Zustand
    useGameStore.getState().setMyPlayer('PLAYER_2');
    useGameStore.getState().setRoomId(cleanRoomId);
  }

  public sendMove(moveRequest: MoveRequest): void {
    const roomId = useGameStore.getState().roomId;
    if (!roomId || !this.client.connected) {
      console.error('Không thể gửi nước đi: chưa vào phòng hoặc chưa kết nối');
      return;
    }
    this.client.publish({
      destination: '/app/match/' + roomId + '/move',
      body: JSON.stringify(moveRequest),
    });
  }
}

export const webSocketService = WebSocketService.getInstance();
