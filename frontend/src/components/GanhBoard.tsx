import React, { useState } from 'react';
import { useGameStore } from '../store/useGameStore';
import { webSocketService } from '../services/WebSocketService';

interface Position {
  x: number;
  y: number;
}

export const GanhBoard: React.FC = () => {
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);
  const [selectedPos, setSelectedPos] = useState<Position | null>(null);

  const pieces = gameState?.pieces || {};

  // Parse key chính xác theo định dạng server: "Position[x=0, y=0]"
  const getPiece = (x: number, y: number) => {
    return pieces[`Position[x=${x}, y=${y}]`];
  };

  const handleCellClick = (x: number, y: number) => {
    const isOngoing = gameState?.matchStatus === 'ONGOING';
    const isMyTurn = gameState?.currentPlayer === myPlayer;

    if (!isOngoing || !isMyTurn) return;

    const clickedPiece = getPiece(x, y);

    if (clickedPiece) {
      // Click vào quân của mình
      if (clickedPiece.player === myPlayer) {
        if (selectedPos && selectedPos.x === x && selectedPos.y === y) {
          // Hủy chọn nếu click lại chính quân đó
          setSelectedPos(null);
        } else {
          // Chọn quân mới
          setSelectedPos({ x, y });
        }
      }
    } else {
      // Click ô trống và đang có quân được chọn -> gửi nước đi
      if (selectedPos) {
        webSocketService.sendMove({
          from: selectedPos,
          to: { x, y },
        });
        setSelectedPos(null);
      }
    }
  };

  // Tọa độ % cho mỗi điểm trên lưới 5x5
  const pct = (v: number) => `${v * 25}%`;

  // Các đường kẻ SVG cho lưới Cờ Gánh (Alquerque)
  // 5 đường ngang, 5 đường dọc, 2 chéo chính, 4 chéo phụ (hình thoi ở trung tâm)
  const lines: { x1: string; y1: string; x2: string; y2: string }[] = [];

  // 5 đường ngang (y = 0,1,2,3,4)
  for (let row = 0; row <= 4; row++) {
    lines.push({ x1: '0%', y1: pct(row), x2: '100%', y2: pct(row) });
  }

  // 5 đường dọc (x = 0,1,2,3,4)
  for (let col = 0; col <= 4; col++) {
    lines.push({ x1: pct(col), y1: '0%', x2: pct(col), y2: '100%' });
  }

  // 2 chéo chính: góc trên-trái -> góc dưới-phải và góc trên-phải -> góc dưới-trái
  lines.push({ x1: '0%', y1: '0%', x2: '100%', y2: '100%' });
  lines.push({ x1: '100%', y1: '0%', x2: '0%', y2: '100%' });

  // 4 chéo phụ tạo hình thoi: nối 4 trung điểm của 4 cạnh ngoài cùng
  // Trung điểm: trên (2,0), phải (4,2), dưới (2,4), trái (0,2)
  lines.push({ x1: '50%', y1: '0%', x2: '100%', y2: '50%' });
  lines.push({ x1: '100%', y1: '50%', x2: '50%', y2: '100%' });
  lines.push({ x1: '50%', y1: '100%', x2: '0%', y2: '50%' });
  lines.push({ x1: '0%', y1: '50%', x2: '50%', y2: '0%' });

  return (
    <div className="relative w-full aspect-square max-w-[500px] mx-auto bg-[#e7dfd5] border-4 border-stone-900 p-4">
      <div className="relative w-full h-full">
        {/* Lớp 1: Đường kẻ SVG */}
        <svg
          className="absolute inset-0 w-full h-full"
          overflow="visible"
          aria-hidden="true"
        >
          {lines.map((line, idx) => (
            <line
              key={idx}
              x1={line.x1}
              y1={line.y1}
              x2={line.x2}
              y2={line.y2}
              stroke="currentColor"
              className="text-stone-800"
              strokeWidth="2"
            />
          ))}
        </svg>

        {/* Lớp 2: Quân cờ Absolute */}
        {Array.from({ length: 5 }, (_, y) =>
          Array.from({ length: 5 }, (_, x) => {
            const piece = getPiece(x, y);
            const isSelected = selectedPos?.x === x && selectedPos?.y === y;
            const isMyTurnNow =
              gameState?.matchStatus === 'ONGOING' &&
              gameState?.currentPlayer === myPlayer;
            const isMyPiece = piece?.player === myPlayer;
            const isEmptyClickable = !piece && !!selectedPos && isMyTurnNow;
            const isPieceClickable = !!piece && isMyPiece && isMyTurnNow;

            return (
              <div
                key={`${x}-${y}`}
                onClick={() => handleCellClick(x, y)}
                role="button"
                aria-label={`Điểm hàng ${y + 1} cột ${x + 1}${piece ? (piece.player === 'PLAYER_1' ? ' - Quân Đỏ' : ' - Quân Đen') : ' - Trống'}`}
                style={{ left: pct(x), top: pct(y) }}
                className={`absolute -translate-x-1/2 -translate-y-1/2 w-8 h-8 sm:w-10 sm:h-10 flex items-center justify-center z-10
                  ${isEmptyClickable || isPieceClickable ? 'cursor-pointer' : 'cursor-default'}`}
              >
                {piece ? (
                  <div
                    className={`w-full h-full rounded-full border-2 border-stone-900
                      shadow-[inset_0_-3px_0_rgba(0,0,0,0.3)] transition-transform hover:scale-105
                      ${piece.player === 'PLAYER_1' ? 'bg-red-700' : 'bg-stone-800'}
                      ${isSelected ? 'ring-4 ring-yellow-400 ring-offset-2 ring-offset-[#e7dfd5]' : ''}
                    `}
                  />
                ) : (
                  <div
                    className={`w-3 h-3 rounded-full transition-colors
                      ${isEmptyClickable ? 'bg-stone-500 hover:bg-stone-700' : 'bg-stone-400'}
                    `}
                  />
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
