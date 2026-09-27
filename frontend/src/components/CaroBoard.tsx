import React from 'react';
import { useGameStore, type Piece } from '../store/useGameStore';
import { webSocketService } from '../services/WebSocketService';

const BOARD_SIZE = 15;

export const CaroBoard: React.FC = () => {
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);

  const pieces = gameState?.pieces || {};

  // Parse Key dựa trên định dạng chính xác từ server: "Position[x=7, y=7]"
  const getPiece = (x: number, y: number): Piece | undefined => {
    return pieces[`Position[x=${x}, y=${y}]`];
  };

  const handleCellClick = (x: number, y: number) => {
    // Logic Click: Chỉ gọi sendMove khi matchStatus === 'ONGOING', ô đang trống, VÀ currentPlayer === myPlayer
    const isOngoing = gameState?.matchStatus === 'ONGOING';
    const isMyTurn = gameState?.currentPlayer === myPlayer;
    const isOccupied = !!getPiece(x, y);

    if (!isOngoing || !isMyTurn || isOccupied) {
      return;
    }

    webSocketService.sendMove({
      from: null,
      to: { x, y },
    });
  };

  // Tạo mảng 225 ô (15x15) theo thứ tự hàng (y: 0->14) và cột (x: 0->14)
  const cells = [];
  for (let y = 0; y < BOARD_SIZE; y++) {
    for (let x = 0; x < BOARD_SIZE; x++) {
      cells.push({ x, y });
    }
  }

  const isMyTurn = gameState?.matchStatus === 'ONGOING' && gameState?.currentPlayer === myPlayer;

  return (
    <div className="w-full max-w-[640px] mx-auto my-4">
      {/* Bàn cờ 15x15: arbitrary grid-cols, viền gỗ đậm, nền đất/mộc, không bo góc, không shadow */}
      <div className="border-4 border-stone-800 bg-[#e7dfd5] p-2">
        <div className="grid grid-cols-[repeat(15,minmax(0,1fr))] border border-stone-800">
          {cells.map(({ x, y }) => {
            const piece = getPiece(x, y);
            const isClickable = isMyTurn && !piece;

            return (
              <button
                key={`${x}-${y}`}
                type="button"
                onClick={() => handleCellClick(x, y)}
                disabled={!isClickable}
                aria-label={`Ô cờ hàng ${y + 1} cột ${x + 1}`}
                className={`aspect-square border border-stone-600 flex items-center justify-center p-0 m-0 leading-none transition-colors
                  ${!piece && isClickable ? 'hover:bg-[#ded4c6] cursor-pointer' : 'cursor-default'}
                  ${!piece ? 'bg-transparent' : 'bg-[#ded4c6]'}
                `}
              >
                {piece?.player === 'PLAYER_1' && (
                  <span className="text-red-700 font-extrabold text-base sm:text-xl md:text-2xl select-none">
                    X
                  </span>
                )}
                {piece?.player === 'PLAYER_2' && (
                  <span className="text-stone-800 font-extrabold text-base sm:text-xl md:text-2xl select-none">
                    O
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
