import { Client, type StompSubscription } from '@stomp/stompjs';
import { type GameStateResponse, type MoveRequest, useGameStore } from '../store/useGameStore';

export class WebSocketService {
  private static instance: WebSocketService;
  private client: Client;
  private matchSubscription: StompSubscription | null = null;
  private eventsSubscription: StompSubscription | null = null;
  private pendingRoomId: string | null = null;
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
          let msg = message.body;
          try {
            const data = JSON.parse(message.body);
            msg = data.message || JSON.stringify(data);
          } catch {
            // Sử dụng raw body nếu không parse được JSON
          }
          useGameStore.getState().setErrorMessage(msg);

          // Nếu nhận lỗi trên /user/queue/errors khi đang có pendingRoomId: hủy subscription, xóa pendingRoomId, ở lại sảnh và hiện lỗi
          if (this.pendingRoomId) {
            this.cleanupMatchSubscription();
            this.pendingRoomId = null;
          }
        });

        // 2. Subscribe /user/queue/room/created khi Host tạo phòng xong
        this.client.subscribe('/user/queue/room/created', (message) => {
          try {
            const data = JSON.parse(message.body);
            const roomId = data.roomId;
            if (roomId) {
              // BẮT BUỘC subscribe /topic/match/{roomId} và events NGAY LẬP TỨC để lắng nghe cập nhật bàn cờ
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

        // 2b. Subscribe /user/queue/room/bot-created khi tạo phòng Đấu với Máy xong.
        // Server trả về luôn GameStateResponse ban đầu (ván đã ONGOING) — không tự dựng state ở client.
        this.client.subscribe('/user/queue/room/bot-created', (message) => {
          try {
            const state: GameStateResponse = JSON.parse(message.body);
            if (!state || !state.roomId || !state.matchStatus) {
              console.warn('Bỏ qua payload tạo phòng máy không hợp lệ:', state);
              return;
            }
            this.subscribeToMatch(state.roomId);
            const store = useGameStore.getState();
            store.setMyPlayer('PLAYER_1');
            store.setIsBotMatch(true);
            store.setRoomGameType(state.gameType);
            store.setGameState(state);
            store.setRoomId(state.roomId);
          } catch (err) {
            console.error('Lỗi khi xử lý room/bot-created:', err);
            useGameStore.getState().setErrorMessage('Không thể xử lý phản hồi tạo phòng đấu máy');
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
        this.pendingRoomId = null;
        this.client.deactivate();
        useGameStore.getState().setIsConnected(false);
      }
      this.disconnectTimer = null;
    }, 100);
  }

  public subscribeToMatch(roomId: string): void {
    this.cleanupMatchSubscription();

    // Topic 1: Trạng thái trận đấu
    this.matchSubscription = this.client.subscribe(
      `/topic/match/${roomId}`,
      (message) => {
        try {
          const state: GameStateResponse = JSON.parse(message.body);

          // Handler topic trạng thái phải kiểm tra payload có matchStatus và (pieces hoặc pieceCounts)
          if (!state || typeof state !== 'object' || !state.matchStatus || (!state.pieces && !state.pieceCounts)) {
            console.warn('Bỏ qua payload trạng thái không hợp lệ:', state);
            return;
          }

          useGameStore.getState().setGameState(state);
          useGameStore.getState().setRoomGameType(state.gameType);

          // Chỉ khi nhận GameStateResponse đầu tiên của đúng roomId đó mới setRoomId + setMyPlayer('PLAYER_2')
          if (this.pendingRoomId && this.pendingRoomId === state.roomId) {
            useGameStore.getState().setRoomId(state.roomId);
            useGameStore.getState().setMyPlayer('PLAYER_2');
            this.pendingRoomId = null;
          }
        } catch (err) {
          console.error('Lỗi parse trạng thái trận đấu:', err);
        }
      }
    );

    // Topic 2: Sự kiện riêng của trận đấu (ví dụ: đối thủ rời phòng)
    this.eventsSubscription = this.client.subscribe(
      `/topic/match/${roomId}/events`,
      (message) => {
        try {
          if (message.body) {
            console.info('Nhận sự kiện trận đấu:', message.body);
          }
          // Handler topic events chỉ đặt cờ opponentLeft trong store, KHÔNG đụng gameState
          useGameStore.getState().setOpponentLeft(true);
        } catch (err) {
          console.error('Lỗi parse sự kiện trận đấu:', err);
        }
      }
    );
  }

  public cleanupMatchSubscription(): void {
    if (this.matchSubscription) {
      this.matchSubscription.unsubscribe();
      this.matchSubscription = null;
    }
    if (this.eventsSubscription) {
      this.eventsSubscription.unsubscribe();
      this.eventsSubscription = null;
    }
  }

  public createRoom(gameType: string = 'CARO'): void {
    if (!this.client.connected) {
      useGameStore.getState().setErrorMessage('Chưa kết nối tới máy chủ WebSocket');
      return;
    }

    useGameStore.getState().setRoomGameType(gameType);

    this.client.publish({
      destination: '/app/room/create',
      body: JSON.stringify({ gameType }),
    });
  }

  public createBotRoom(): void {
    if (!this.client.connected) {
      useGameStore.getState().setErrorMessage('Chưa kết nối tới máy chủ WebSocket');
      return;
    }

    this.client.publish({
      destination: '/app/room/create-bot',
      body: JSON.stringify({ gameType: 'CARO' }),
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

    // Không setRoomId/setMyPlayer trong joinRoom. Lưu pendingRoomId
    this.pendingRoomId = cleanRoomId;

    // 1. BẮT BUỘC subscribe /topic/match/{roomId} và events TRƯỚC
    this.subscribeToMatch(cleanRoomId);

    // 2. Sau đó mới gửi message tới /app/room/join
    this.client.publish({
      destination: '/app/room/join',
      body: JSON.stringify({ roomId: cleanRoomId }),
    });
  }

  public leaveRoom(): void {
    this.cleanupMatchSubscription();
    this.pendingRoomId = null;
    useGameStore.getState().resetGame();
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
