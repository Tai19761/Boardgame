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
  const [pvpBoardSize, setPvpBoardSize] = useState<number>(15);
  const [botBoardSize, setBotBoardSize] = useState<number>(15);
  const [botDifficulty, setBotDifficulty] = useState<string>('NORMAL');

  useEffect(() => {
    webSocketService.connect();
    return () => {
      webSocketService.disconnect();
    };
  }, []);

  const handleCreateRoom = (gameType: 'CARO' | 'GANH' | 'O_AN_QUAN' = 'CARO') => {
    webSocketService.createRoom(gameType, gameType === 'CARO' ? pvpBoardSize : undefined);
  };

  const handleCreateBotRoom = () => {
    webSocketService.createBotRoom(botBoardSize, botDifficulty);
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
      <header className="max-w-4xl mx-auto mb-4 border-b-2 border-stone-800 pb-3 flex items-center justify-between">
        <h1 className="text-xl sm:text-2xl font-black uppercase tracking-wide text-stone-900">
          Cờ Dân Gian
        </h1>
        <div className="text-sm font-semibold flex items-center gap-2">
          <span className="text-stone-600">Máy chủ:</span>
          <span className={isConnected ? 'text-green-800 font-bold' : 'text-red-700 font-bold'}>
            {isConnected ? '● Đã kết nối' : '○ Mất kết nối'}
          </span>
        </div>
      </header>

      {roomId == null ? (
        <div className="max-w-4xl mx-auto border-4 border-stone-800 bg-[#e7dfd5] p-6 text-stone-900">
          <div className="mb-6 border-b border-stone-400 pb-4">
            <h2 className="text-lg font-bold text-stone-900 uppercase">Sảnh Chờ Phòng Đấu</h2>
            <p className="text-sm text-stone-700 mt-1">
              Khởi tạo phòng đấu Caro, Cờ Gánh hoặc Ô Ăn Quan mới, luyện tập với máy hoặc tham gia phòng bằng mã định danh.
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

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
            {/* Cột trái: TẠO PHÒNG MỚI (PvP) */}
            <div className="border-2 border-stone-800 bg-stone-200 p-5 flex flex-col gap-4">
              <div>
                <h3 className="font-extrabold text-stone-900 text-base uppercase mb-1">
                  Tạo Phòng Mới (PvP)
                </h3>
                <p className="text-xs text-stone-700">
                  Tạo phòng thi đấu với người chơi khác. Hệ thống sẽ cấp mã phòng để bạn gửi cho đối thủ.
                </p>
              </div>

              {/* Mục Caro */}
              <div className="p-3 bg-stone-100 border border-stone-400 flex flex-col gap-2.5">
                <div className="flex items-center justify-between gap-2">
                  <label htmlFor="pvp-caro-size" className="text-xs font-bold text-stone-800 uppercase">
                    Bàn Caro:
                  </label>
                  <select
                    id="pvp-caro-size"
                    value={pvpBoardSize}
                    onChange={(e) => setPvpBoardSize(Number(e.target.value))}
                    className="px-2 py-1 border-2 border-stone-800 bg-white text-stone-900 font-bold text-xs focus:outline-none focus:border-stone-950 cursor-pointer"
                  >
                    <option value={15}>15x15 (Tiêu chuẩn)</option>
                    <option value={20}>20x20 (Mở rộng)</option>
                    <option value={25}>25x25 (Đại chiến)</option>
                  </select>
                </div>
                <button
                  type="button"
                  onClick={() => handleCreateRoom('CARO')}
                  disabled={!isConnected}
                  className="w-full px-4 py-2.5 bg-stone-800 text-stone-100 font-bold border-2 border-stone-950 hover:bg-stone-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase text-sm"
                >
                  + Tạo phòng Caro ({pvpBoardSize}x{pvpBoardSize})
                </button>
              </div>

              {/* Mục Cờ Gánh & Ô Ăn Quan (độc lập, không có dropdown) */}
              <div className="flex flex-col gap-3">
                <button
                  type="button"
                  onClick={() => handleCreateRoom('GANH')}
                  disabled={!isConnected}
                  className="w-full px-4 py-3 bg-red-800 text-stone-100 font-bold border-2 border-red-950 hover:bg-red-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase text-sm"
                >
                  + Tạo phòng Cờ Gánh
                </button>
                <button
                  type="button"
                  onClick={() => handleCreateRoom('O_AN_QUAN')}
                  disabled={!isConnected}
                  className="w-full px-4 py-3 bg-amber-800 text-stone-100 font-bold border-2 border-amber-950 hover:bg-amber-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase text-sm"
                >
                  + Tạo phòng Ô Ăn Quan
                </button>
              </div>
            </div>

            {/* Cột phải: ĐẤU VỚI MÁY & VÀO PHÒNG ĐÃ CÓ */}
            <div className="flex flex-col gap-6">
              {/* Khối Đấu với Máy (Bot Match) */}
              <div className="border-2 border-stone-800 bg-stone-200 p-5 flex flex-col gap-4">
                <div>
                  <h3 className="font-extrabold text-stone-900 text-base uppercase mb-1">
                    Đấu với Máy (Bot Match)
                  </h3>
                  <p className="text-xs text-stone-700">
                    Luyện tập Caro với máy. Bạn cầm quân X và đi trước, ván đấu bắt đầu ngay lập tức.
                  </p>
                </div>

                {/* Khung cấu hình liền kề: Kích thước bàn & Cấp độ Bot */}
                <div className="p-3 bg-stone-100 border border-stone-400 flex flex-col gap-3">
                  <div className="flex items-center justify-between gap-2">
                    <label htmlFor="bot-board-size" className="text-xs font-bold text-stone-800 uppercase">
                      Kích thước bàn:
                    </label>
                    <select
                      id="bot-board-size"
                      value={botBoardSize}
                      onChange={(e) => setBotBoardSize(Number(e.target.value))}
                      className="px-2 py-1 border-2 border-stone-800 bg-white text-stone-900 font-bold text-xs focus:outline-none focus:border-stone-950 cursor-pointer"
                    >
                      <option value={15}>15x15 (Tiêu chuẩn)</option>
                      <option value={20}>20x20 (Mở rộng)</option>
                      <option value={25}>25x25 (Đại chiến)</option>
                    </select>
                  </div>

                  <div className="flex items-center justify-between gap-2">
                    <label htmlFor="bot-difficulty-select" className="text-xs font-bold text-stone-800 uppercase">
                      Cấp độ Bot:
                    </label>
                    <select
                      id="bot-difficulty-select"
                      value={botDifficulty}
                      onChange={(e) => setBotDifficulty(e.target.value)}
                      className="px-2 py-1 border-2 border-stone-800 bg-white text-stone-900 font-bold text-xs focus:outline-none focus:border-stone-950 cursor-pointer"
                    >
                      <option value="EASY">Dễ (Tập sự)</option>
                      <option value="NORMAL">Bình thường (Depth 3)</option>
                      <option value="HARD">Khó (Depth 4)</option>
                      <option value="EXPERT">Cực khó (Depth 5 + TT)</option>
                    </select>
                  </div>

                  <div className="text-[11px] text-stone-600 bg-stone-200 p-1.5 border border-stone-300 font-mono">
                    {botDifficulty === 'EASY' && '• Dễ: Bot tập sự, biết chặn thua và ăn thắng ngay.'}
                    {botDifficulty === 'NORMAL' && '• Bình thường: Cấu hình chuẩn, phản hồi tức thì.'}
                    {botDifficulty === 'HARD' && '• Khó: Duyệt sâu 4 nước, sắp xếp nước đi tối ưu.'}
                    {botDifficulty === 'EXPERT' && '• Cực khó: Duyệt sâu 5 nước, bảng băm Zobrist.'}
                  </div>
                </div>

                <button
                  id="create-bot-room-button"
                  type="button"
                  onClick={handleCreateBotRoom}
                  disabled={!isConnected}
                  className="w-full px-4 py-3 bg-emerald-900 text-stone-100 font-bold border-2 border-emerald-950 hover:bg-emerald-800 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase text-sm"
                >
                  ⚙ Đấu Caro với Máy ({botBoardSize}x{botBoardSize} - {botDifficulty === 'EASY' ? 'Dễ' : botDifficulty === 'NORMAL' ? 'Bình thường' : botDifficulty === 'HARD' ? 'Khó' : 'Cực khó'})
                </button>
              </div>

              {/* Khối Vào Phòng Đã Có */}
              <div className="border-2 border-stone-800 bg-stone-200 p-5">
                <h3 className="font-extrabold text-stone-900 text-base uppercase mb-1">
                  Vào Phòng Đã Có
                </h3>
                <p className="text-xs text-stone-700 mb-3">
                  Nhập mã phòng (UUID) do người chơi khác cung cấp để tham gia ván đấu.
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
                    className="px-6 py-2 bg-stone-800 text-stone-100 font-bold border-2 border-stone-950 hover:bg-stone-700 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed uppercase text-sm"
                  >
                    Vào Phòng
                  </button>
                </div>
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
