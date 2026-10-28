import React from 'react';
import { useGameStore, type Piece } from '../store/useGameStore';
import { webSocketService } from '../services/WebSocketService';

const ALLOWED_SIZES = [15, 20, 25] as const;

// Object map chứa chuỗi class Tailwind đầy đủ (CẤM ghép chuỗi động để không bị loại bỏ khi build)
const FONT_SIZE_MAP: Record<number, string> = {
  15: 'text-base sm:text-xl md:text-2xl',
  20: 'text-xs sm:text-sm md:text-base',
  25: 'text-[10px] sm:text-xs md:text-sm',
};

const MIN_WIDTH_MAP: Record<number, string> = {
  15: 'min-w-[320px]',
  20: 'min-w-[460px]',
  25: 'min-w-[560px]',
};

export const CaroBoard: React.FC = () => {
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);
  const opponentLeft = useGameStore((state) => state.opponentLeft);

  // Chuẩn hóa boardSize từ server, nếu dị thường thì ép về 15
  const rawSize = gameState?.boardSize;
  const boardSize = (typeof rawSize === 'number' && (ALLOWED_SIZES as readonly number[]).includes(rawSize))
    ? rawSize
    : 15;

  const fontClass = FONT_SIZE_MAP[boardSize] || FONT_SIZE_MAP[15];
  const minWidthClass = MIN_WIDTH_MAP[boardSize] || MIN_WIDTH_MAP[15];

  const pieces = gameState?.pieces || {};

  // Parse Key dựa trên định dạng chính xác từ server: "Position[x=7, y=7]"
  const getPiece = (x: number, y: number): Piece | undefined => {
    return pieces[`Position[x=${x}, y=${y}]`];
  };

  const handleCellClick = (x: number, y: number) => {
    // Logic Click: Chỉ gọi sendMove khi matchStatus === 'ONGOING', ô đang trống, currentPlayer === myPlayer và đối thủ chưa rời
    const isOngoing = gameState?.matchStatus === 'ONGOING';
    const isMyTurn = gameState?.currentPlayer === myPlayer;
    const isOccupied = !!getPiece(x, y);

    if (!isOngoing || !isMyTurn || isOccupied || opponentLeft) {
      return;
    }

    webSocketService.sendMove({
      from: null,
      to: { x, y },
    });
  };

  // Tạo mảng size * size ô theo thứ tự hàng (y: 0->size-1) và cột (x: 0->size-1)
  const cells = [];
  for (let y = 0; y < boardSize; y++) {
    for (let x = 0; x < boardSize; x++) {
      cells.push({ x, y });
    }
  }

  const isMyTurn = gameState?.matchStatus === 'ONGOING' && gameState?.currentPlayer === myPlayer;

  return (
    <div className="w-full max-w-[720px] mx-auto my-4 overflow-x-auto pb-2">
      {/* Bàn cờ Caro động: CSS gridTemplateColumns inline, viền gỗ đậm, min-width chống vỡ giao diện trên mobile */}
      <div className={`border-4 border-stone-800 bg-[#e7dfd5] p-2 mx-auto ${minWidthClass}`}>
        <div
          className="grid border border-stone-800"
          style={{ gridTemplateColumns: `repeat(${boardSize}, minmax(0, 1fr))` }}
        >
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
                  <span className={`text-red-700 font-extrabold select-none ${fontClass}`}>
                    X
                  </span>
                )}
                {piece?.player === 'PLAYER_2' && (
                  <span className={`text-stone-800 font-extrabold select-none ${fontClass}`}>
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
