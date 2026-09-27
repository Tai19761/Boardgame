import { useEffect, useState } from 'react';
import { useGameStore } from './store/useGameStore';
import { webSocketService } from './services/WebSocketService';
import { GameRoom } from './components/GameRoom';

export default function App() {
  const isConnected = useGameStore((state) => state.isConnected);
  const roomId = useGameStore((state) => state.roomId);
  const errorMessage = useGameStore((state) => state.errorMessage);
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
    <main className="min-h-screen bg-stone-100 text-stone-900 p-4 font-mono">
      <header className="max-w-[720px] mx-auto mb-4 border-b-2 border-stone-800 pb-3 flex items-center justify-between">
        <h1 className="text-xl sm:text-2xl font-black uppercase tracking-wide text-stone-900">
          Cờ Dân Gian - Ca-rô
        </h1>
        <div className="text-sm font-semibold flex items-center gap-2">
          <span className="text-stone-600">Máy chủ:</span>
          <span className={isConnected ? 'text-green-800 font-bold' : 'text-red-700 font-bold'}>
            {isConnected ? '● Đã kết nối' : '○ Mất kết nối'}
          </span>
        </div>
      </header>

      {roomId == null ? (
        <div className="max-w-[720px] mx-auto border-4 border-stone-800 bg-[#e7dfd5] p-6 text-stone-900">
          <div className="mb-6 border-b border-stone-400 pb-4">
            <h2 className="text-lg font-bold text-stone-900 uppercase">Sảnh Chờ Phòng Đấu</h2>
            <p className="text-sm text-stone-700 mt-1">
              Khởi tạo phòng đấu Caro mới hoặc tham gia phòng bằng mã định danh.
            </p>
          </div>

          {/* Hiển thị lỗi nếu có */}
          {errorMessage && (
            <div className="bg-red-800 text-white border-2 border-red-950 p-3 mb-6 flex items-center justify-between">
              <span className="font-medium text-sm sm:text-base">Lỗi: {errorMessage}</span>
              <button
                type="button"
                onClick={clearError}
                className="border border-white bg-red-900 px-2 py-0.5 text-white font-bold cursor-pointer hover:bg-red-950 ml-3"
                aria-label="Đóng thông báo lỗi"
              >
                X
              </button>
            </div>
          )}

          <div className="space-y-6">
            {/* Khối 1: Tạo Phòng Mới */}
            <div className="border-2 border-stone-800 bg-stone-200 p-5">
              <h3 className="font-extrabold text-stone-900 text-base uppercase mb-1">
                Tạo Phòng Mới
              </h3>
              <p className="text-sm text-stone-700 mb-4">
                Khởi tạo phòng đấu Caro mới, hệ thống sẽ cấp mã phòng để chia sẻ cho đối thủ.
              </p>
              <button
                type="button"
                onClick={handleCreateRoom}
                disabled={!isConnected}
                className="w-full sm:w-auto px-6 py-3 bg-stone-800 text-stone-100 font-bold border-2 border-stone-950 hover:bg-stone-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase"
              >
                + Tạo phòng Cờ Caro
              </button>
            </div>

            {/* Khối 2: Vào Phòng Đã Có */}
            <div className="border-2 border-stone-800 bg-stone-200 p-5">
              <h3 className="font-extrabold text-stone-900 text-base uppercase mb-1">
                Vào Phòng Đã Có
              </h3>
              <p className="text-sm text-stone-700 mb-3">
                Nhập mã phòng (UUID) do người chơi thứ nhất cung cấp để tham gia ván đấu.
              </p>
              <div className="flex flex-col sm:flex-row gap-2">
                <input
                  id="room-id-input"
                  type="text"
                  placeholder="Dán mã phòng UUID tại đây..."
                  value={inputRoomId}
                  onChange={(e) => setInputRoomId(e.target.value)}
                  className="flex-1 px-3 py-2 border-2 border-stone-700 bg-white text-stone-900 font-mono text-sm focus:outline-none focus:border-stone-950"
                />
                <button
                  type="button"
                  onClick={handleJoinRoom}
                  disabled={!isConnected}
                  className="px-6 py-2 bg-stone-800 text-stone-100 font-bold border-2 border-stone-950 hover:bg-stone-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase"
                >
                  Vào Phòng
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <GameRoom />
      )}
    </main>
  );
}
