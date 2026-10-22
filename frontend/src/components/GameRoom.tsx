import React from 'react';
import { useGameStore } from '../store/useGameStore';
import { webSocketService } from '../services/WebSocketService';
import { CaroBoard } from './CaroBoard';
import { GanhBoard } from './GanhBoard';

export const GameRoom: React.FC = () => {
  const roomId = useGameStore((state) => state.roomId);
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);
  const opponentLeft = useGameStore((state) => state.opponentLeft);
  const errorMessage = useGameStore((state) => state.errorMessage);
  const clearError = useGameStore((state) => state.clearError);
  const roomGameType = useGameStore((state) => state.roomGameType);

  const handleExit = () => {
    const { gameState, opponentLeft } = useGameStore.getState();
    const isGameOver = gameState !== null && gameState.matchStatus !== 'ONGOING';

    if (isGameOver || opponentLeft) {
      // Ván đã kết thúc hoặc đối thủ đã rời: server đã dọn phòng, gọi leaveRoom()
      webSocketService.leaveRoom();
    } else {
      // Đang chờ đối thủ hoặc đang chơi: dùng reload() để ngắt kết nối, server kích hoạt handlePlayerDisconnect
      // sau này thay bằng lệnh /app/room/leave khi làm nút đầu hàng
      window.location.reload();
    }
  };

  if (!roomId) {
    return null;
  }

  // Xác định loại game từ Zustand store (có sẵn ngay khi tạo/join phòng)
  const gameType = roomGameType || 'CARO';
  const isGanh = gameType === 'GANH';

  // Dynamic labels theo game type
  const labelP1 = isGanh ? 'Đỏ' : 'X';
  const labelP2 = isGanh ? 'Đen' : 'O';
  const myLabel = myPlayer === 'PLAYER_1' ? labelP1 : myPlayer === 'PLAYER_2' ? labelP2 : '?';

  // Nếu !gameState, hiển thị trạng thái chờ hoặc tải
  if (!gameState) {
    return (
      <div className="max-w-[720px] mx-auto my-6 p-6 border-4 border-stone-800 bg-[#e7dfd5] text-stone-900">
        {/* Hiển thị lỗi nếu có */}
        {errorMessage && (
          <div className="bg-red-800 text-white border-2 border-red-950 p-3 mb-4 flex items-center justify-between">
            <span className="font-medium">Lỗi: {errorMessage}</span>
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

        <div className="border-2 border-stone-800 bg-stone-200 p-4 mb-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-stone-400 pb-2 mb-2">
            <div>
              <span className="font-bold text-sm text-stone-700">Mã phòng: </span>
              <code className="font-mono text-sm font-bold break-all">{roomId}</code>
            </div>
            <button
              type="button"
              onClick={handleExit}
              className="self-start sm:self-auto border-2 border-stone-900 bg-stone-300 hover:bg-stone-400 text-stone-900 font-bold px-3 py-1 text-xs sm:text-sm cursor-pointer"
            >
              ⬅ Quay lại Sảnh
            </button>
          </div>
          <p className="text-base mt-2">
            <strong className="font-extrabold">
              BẠN ĐANG CẦM QUÂN:{' '}
              <span className={`text-xl ${myPlayer === 'PLAYER_1' ? 'text-red-700' : 'text-stone-800'}`}>
                {myLabel}
              </span>
            </strong>
          </p>
        </div>

        <div className="p-4 border-2 border-stone-600 bg-stone-100 text-stone-800">
          <p className="font-bold">Đang tải dữ liệu...</p>
          <p className="text-sm text-stone-600 mt-1">Đang chờ người chơi thứ hai tham gia để bắt đầu ván đấu.</p>
        </div>
      </div>
    );
  }

  // Format trạng thái ván đấu - dynamic label theo game type
  const formatStatus = () => {
    switch (gameState.matchStatus) {
      case 'ONGOING':
        return opponentLeft ? 'Ván đã kết thúc' : 'Đang diễn ra';
      case 'WIN_PLAYER_1':
        return `Ván đã kết thúc - ${labelP1} thắng`;
      case 'WIN_PLAYER_2':
        return `Ván đã kết thúc - ${labelP2} thắng`;
      case 'DRAW':
        return 'Hòa cờ';
      default:
        return gameState.matchStatus;
    }
  };

  const isGameOver = gameState.matchStatus.includes('WIN') || gameState.matchStatus === 'DRAW';
  const isWinner = gameState.winner === myPlayer;
  const isDraw = gameState.matchStatus === 'DRAW';

  const currentTurnLabel =
    gameState.currentPlayer === 'PLAYER_1'
      ? labelP1
      : gameState.currentPlayer === 'PLAYER_2'
      ? labelP2
      : '-';

  return (
    <div className="max-w-[720px] mx-auto my-4 p-4 border-4 border-stone-800 bg-[#e7dfd5] text-stone-900">
      {/* Hiển thị lỗi: thanh đỏ đậm, chữ trắng, có nút X để tắt */}
      {errorMessage && (
        <div className="bg-red-800 text-white border-2 border-red-950 p-3 mb-4 flex items-center justify-between">
          <span className="font-medium">Lỗi: {errorMessage}</span>
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

      {/* Bảng Trạng Thái: Tên phòng, Trạng thái, Lượt của, BẠN ĐANG CẦM QUÂN, Nút Rời phòng */}
      <div className="border-2 border-stone-800 bg-stone-200 p-4 mb-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-stone-400 pb-2 mb-2 gap-2">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-stone-700 text-sm">Phòng: </span>
            <code className="font-mono font-bold text-xs sm:text-sm break-all">{roomId}</code>
            <button
              type="button"
              onClick={handleExit}
              className="border-2 border-stone-900 bg-stone-300 hover:bg-stone-400 text-stone-900 font-bold px-2 py-0.5 text-xs cursor-pointer ml-1"
            >
              ⬅ Rời phòng
            </button>
          </div>
          <div>
            <span className="text-stone-700 text-sm">Trạng thái: </span>
            <span className="font-bold text-stone-900">{formatStatus()}</span>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pt-1 gap-2">
          {!isGameOver && !opponentLeft && (
            <div className="text-base">
              <span>Lượt của: </span>
              <span className={`font-extrabold text-lg ${gameState.currentPlayer === 'PLAYER_1' ? 'text-red-700' : 'text-stone-800'}`}>
                {currentTurnLabel} {gameState.currentPlayer === myPlayer ? '(Lượt của bạn)' : '(Đối thủ)'}
              </span>
            </div>
          )}

          <div className="text-base">
            <strong className="text-stone-900 font-extrabold">
              BẠN ĐANG CẦM QUÂN:{' '}
              <span className={`text-xl ${myPlayer === 'PLAYER_1' ? 'text-red-700' : 'text-stone-800'}`}>
                {myLabel}
              </span>
            </strong>
          </div>
        </div>
      </div>

      {/* Khung đối thủ rời ván */}
      {opponentLeft && (
        <div className="p-5 mb-4 text-center border-4 border-amber-800 bg-amber-100 text-stone-900">
          <p className="text-lg sm:text-xl font-black text-amber-950 mb-4">
            Đối thủ đã rời ván. Ván đấu kết thúc, bạn được xử thắng.
          </p>
          <button
            type="button"
            onClick={handleExit}
            className="px-8 py-3 bg-stone-900 hover:bg-stone-800 text-stone-100 font-extrabold text-base border-2 border-stone-950 cursor-pointer uppercase transition-colors"
          >
            ⬅ Về sảnh
          </button>
        </div>
      )}

      {/* Banner Tuyên bố Thắng / Thua / Hòa */}
      {isGameOver && (
        <div className="p-5 mb-4 text-center border-4 border-stone-800 bg-stone-200">
          <div className="mb-4">
            {isDraw ? (
              <span className="text-xl sm:text-2xl font-black text-stone-800 tracking-wide">
                🤝 HÒA CỜ!
              </span>
            ) : isWinner ? (
              <span className="text-xl sm:text-2xl font-black text-red-700 tracking-wide">
                🏆 BẠN ĐÃ CHIẾN THẮNG!
              </span>
            ) : (
              <span className="text-xl sm:text-2xl font-black text-stone-700 tracking-wide">
                💀 BẠN ĐÃ THUA!
              </span>
            )}
          </div>
          <div>
            <button
              type="button"
              onClick={handleExit}
              className="px-8 py-3 bg-stone-900 hover:bg-stone-800 text-stone-100 font-extrabold text-base border-2 border-stone-950 cursor-pointer uppercase transition-colors"
            >
              ⬅ Về sảnh
            </button>
          </div>
        </div>
      )}

      {/* Bàn cờ: động theo gameType */}
      <div className={opponentLeft || isGameOver ? 'pointer-events-none opacity-60' : ''}>
        {isGanh ? <GanhBoard /> : <CaroBoard />}
      </div>
    </div>
  );
};
