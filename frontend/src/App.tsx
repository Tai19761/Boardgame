import { useEffect, useState } from 'react';
import { useGameStore } from './store/useGameStore';
import { webSocketService } from './services/WebSocketService';

export default function App() {
  const isConnected = useGameStore((state) => state.isConnected);
  const roomId = useGameStore((state) => state.roomId);
  const errorMessage = useGameStore((state) => state.errorMessage);
  const gameState = useGameStore((state) => state.gameState);
  const clearError = useGameStore((state) => state.clearError);

  const [inputRoomId, setInputRoomId] = useState('');

  useEffect(() => {
    webSocketService.connect();
    return () => {
      webSocketService.disconnect();
    };
  }, []);

  const handleCreateRoom = () => {
    webSocketService.createRoom('CARO');
  };

  const handleJoinRoom = () => {
    if (!inputRoomId.trim()) {
      useGameStore.getState().setErrorMessage('Vui lòng nhập mã phòng');
      return;
    }
    webSocketService.joinRoom(inputRoomId.trim());
  };

  return (
    <div style={{ maxWidth: '800px', margin: '20px auto', padding: '16px', border: '1px solid #ccc' }}>
      <h2>Web Game Cờ Truyền Thống - Giao diện Debug STOMP</h2>

      <div style={{ marginBottom: '16px' }}>
        <span>Trạng thái WebSocket: </span>
        <span style={{ fontWeight: 'bold', color: isConnected ? 'green' : 'red' }}>
          {isConnected ? 'Connected' : 'Disconnected'}
        </span>
      </div>

      {errorMessage && (
        <div style={{ padding: '8px', marginBottom: '16px', border: '1px solid red', backgroundColor: '#ffe6e6', color: 'red' }}>
          <span>Lỗi: {errorMessage}</span>
          <button onClick={clearError} style={{ marginLeft: '12px' }}>
            Xóa lỗi
          </button>
        </div>
      )}

      {roomId == null ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <button onClick={handleCreateRoom} disabled={!isConnected}>
              Tạo phòng Cờ Caro
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center' }}>
            <input
              type="text"
              placeholder="Nhập mã phòng..."
              value={inputRoomId}
              onChange={(e) => setInputRoomId(e.target.value)}
              style={{ width: '280px' }}
            />
            <button onClick={handleJoinRoom} disabled={!isConnected}>
              Vào Phòng
            </button>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ padding: '8px', backgroundColor: '#f0f0f0', marginBottom: '16px', border: '1px solid #999' }}>
            <strong>Mã phòng hiện tại: </strong>
            <code>{roomId}</code>
          </div>

          <div>
            <h3>Raw GameState JSON:</h3>
            <pre
              style={{
                backgroundColor: '#f8f8f8',
                padding: '12px',
                border: '1px solid #ddd',
                overflowX: 'auto',
                fontSize: '13px',
                lineHeight: '1.4',
              }}
            >
              {gameState ? JSON.stringify(gameState, null, 2) : 'Chưa có dữ liệu gameState (Đang chờ guest vào phòng...)'}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
