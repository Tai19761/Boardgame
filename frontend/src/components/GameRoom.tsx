import React from 'react';
import { useGameStore } from '../store/useGameStore';
import { CaroBoard } from './CaroBoard';

export const GameRoom: React.FC = () => {
  const roomId = useGameStore((state) => state.roomId);
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);
  const errorMessage = useGameStore((state) => state.errorMessage);
  const clearError = useGameStore((state) => state.clearError);

  const handleExit = () => {
    window.location.reload();
  };

  if (!roomId) {
    return null;
  }

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
                {myPlayer === 'PLAYER_1' ? 'X' : 'O'}
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

  // Format trạng thái ván đấu
  const formatStatus = () => {
    switch (gameState.matchStatus) {
      case 'ONGOING':
        return 'Đang diễn ra';
      case 'WIN_PLAYER_1':
        return 'Người chơi 1 (X) Thắng';
      case 'WIN_PLAYER_2':
        return 'Người chơi 2 (O) Thắng';
      case 'DRAW':
        return 'Hòa cờ';
      default:
        return gameState.matchStatus;
    }
  };

  const isGameOver = gameState.matchStatus.includes('WIN') || gameState.matchStatus === 'DRAW';
  const isWinner = gameState.winner === myPlayer;
  const isDraw = gameState.matchStatus === 'DRAW';

  const currentTurnChar = gameState.currentPlayer === 'PLAYER_1' ? 'X' : gameState.currentPlayer === 'PLAYER_2' ? 'O' : '-';
  const myChar = myPlayer === 'PLAYER_1' ? 'X' : myPlayer === 'PLAYER_2' ? 'O' : '?';

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
          <div className="text-base">
            <span>Lượt của: </span>
            <span className={`font-extrabold text-lg ${currentTurnChar === 'X' ? 'text-red-700' : 'text-stone-800'}`}>
              {currentTurnChar} {gameState.currentPlayer === myPlayer ? '(Lượt của bạn)' : '(Đối thủ)'}
            </span>
          </div>

          <div className="text-base">
            <strong className="text-stone-900 font-extrabold">
              BẠN ĐANG CẦM QUÂN:{' '}
              <span className={`text-xl ${myChar === 'X' ? 'text-red-700' : 'text-stone-800'}`}>
                {myChar}
              </span>
            </strong>
          </div>
        </div>
      </div>

      {/* Banner Tuyên bố Thắng / Thua / Hòa */}
      {isGameOver && (
        <div className="p-4 mb-4 text-center border-2 border-stone-800 bg-stone-200">
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
      )}

      {/* Bàn cờ Caro */}
      <CaroBoard />
    </div>
  );
};
