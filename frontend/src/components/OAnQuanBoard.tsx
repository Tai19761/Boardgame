import React, { useState } from 'react';
import { useGameStore, type Position } from '../store/useGameStore';
import { webSocketService } from '../services/WebSocketService';

export const OAnQuanBoard: React.FC = () => {
  const gameState = useGameStore((state) => state.gameState);
  const myPlayer = useGameStore((state) => state.myPlayer);
  const opponentLeft = useGameStore((state) => state.opponentLeft);
  const [selectedPos, setSelectedPos] = useState<Position | null>(null);

  const pieceCounts = gameState?.pieceCounts || {};

  // Lấy số lượng sỏi theo tọa độ, hỗ trợ mọi format key từ backend
  const getCount = (x: number, y: number): number => {
    const key1 = `Position[x=${x}, y=${y}]`;
    if (pieceCounts[key1] !== undefined) return pieceCounts[key1];
    const key2 = `${x},${y}`;
    if (pieceCounts[key2] !== undefined) return pieceCounts[key2];
    for (const [k, v] of Object.entries(pieceCounts)) {
      if (k.includes(`x=${x}`) && k.includes(`y=${y}`)) {
        return v;
      }
    }
    return 0;
  };

  const isOngoing = gameState?.matchStatus === 'ONGOING';
  const isMyTurn = isOngoing && gameState?.currentPlayer === myPlayer && !opponentLeft;

  // Kiểm tra ô dân có thể click chọn hay không
  const canSelectCell = (x: number, y: number): boolean => {
    if (!isMyTurn) return false;
    if (myPlayer === 'PLAYER_1' && y !== 1) return false;
    if (myPlayer === 'PLAYER_2' && y !== 0) return false;
    return getCount(x, y) > 0;
  };

  const handleCellClick = (x: number, y: number) => {
    if (!canSelectCell(x, y)) return;
    if (selectedPos && selectedPos.x === x && selectedPos.y === y) {
      setSelectedPos(null);
    } else {
      setSelectedPos({ x, y });
    }
  };

  // Tính tọa độ ô liền kề xác định hướng đi
  const getAdjacentPosition = (from: Position, dir: 'LEFT' | 'RIGHT'): Position => {
    if (dir === 'LEFT') {
      return from.x === 1 ? { x: 0, y: 0 } : { x: from.x - 1, y: from.y };
    } else {
      return from.x === 5 ? { x: 6, y: 0 } : { x: from.x + 1, y: from.y };
    }
  };

  // Tương tác bước 2: chọn hướng và gửi nước đi lên server (KHÔNG Optimistic Update)
  const handleChooseDirection = (e: React.MouseEvent, dir: 'LEFT' | 'RIGHT') => {
    e.stopPropagation();
    if (!selectedPos) return;

    const to = getAdjacentPosition(selectedPos, dir);
    webSocketService.sendMove({
      from: selectedPos,
      to,
      direction: dir,
    });
    setSelectedPos(null);
  };

  // 5 ô dân hàng trên (Player 2: y = 0, x từ 1 đến 5)
  const p2Cells = [1, 2, 3, 4, 5].map((x) => ({ x, y: 0 }));
  // 5 ô dân hàng dưới (Player 1: y = 1, x từ 1 đến 5)
  const p1Cells = [1, 2, 3, 4, 5].map((x) => ({ x, y: 1 }));

  return (
    <div className="w-full max-w-[760px] mx-auto my-4 p-3 sm:p-4 bg-[#e7dfd5] border-4 border-stone-800 select-none">
      <div className="grid grid-cols-[minmax(70px,1.25fr)_repeat(5,minmax(0,1fr))_minmax(70px,1.25fr)] grid-rows-2 gap-1.5 sm:gap-2">
        {/* 1. Ô Quan Trái (Position: 0, 0) - Bo tròn bên trái */}
        <div
          className="col-start-1 row-start-1 row-span-2 rounded-l-full border-4 border-stone-800 bg-[#d8ccbc] flex flex-col items-center justify-center p-2 text-center"
          aria-label={`Ô Quan Trái - ${getCount(0, 0)} sỏi`}
        >
          <span className="text-[10px] sm:text-xs font-black tracking-wider text-stone-700 uppercase">
            Quan Trái
          </span>
          <div className="my-1 flex items-center justify-center">
            <span className="text-2xl sm:text-3xl font-black text-stone-900">
              {getCount(0, 0)}
            </span>
          </div>
          <span className="text-[10px] sm:text-xs font-semibold text-stone-600">
            sỏi
          </span>
        </div>

        {/* 2. Hàng trên: 5 ô dân của Player 2 (y = 0) - Viền dày, góc cạnh Anti-SaaS */}
        {p2Cells.map(({ x, y }, idx) => {
          const count = getCount(x, y);
          const isSelected = selectedPos?.x === x && selectedPos?.y === y;
          const isClickable = canSelectCell(x, y);

          return (
            <div
              key={`p2-${x}-${y}`}
              onClick={() => handleCellClick(x, y)}
              role="button"
              tabIndex={isClickable ? 0 : -1}
              aria-label={`Ô dân Player 2 cột ${x} - ${count} sỏi`}
              className={`col-start-${idx + 2} row-start-1 aspect-square rounded-none border-2 border-stone-800 p-1 sm:p-2 flex flex-col items-center justify-between transition-colors
                ${isSelected ? 'bg-amber-100 border-4 border-amber-800 ring-2 ring-amber-600' : 'bg-[#ded4c6]'}
                ${isClickable && !isSelected ? 'hover:bg-[#d4c8b8] cursor-pointer' : ''}
                ${!isClickable && !isSelected ? 'cursor-default' : ''}
              `}
            >
              <div className="w-full flex items-center justify-between text-[9px] sm:text-[11px] font-bold text-stone-600">
                <span>Dân {x}</span>
                <span className="text-stone-800 font-extrabold">P2</span>
              </div>

              {/* Tương tác 2 bước: Nếu ô đang được chọn -> hiển thị số sỏi và 2 nút Trái / Phải */}
              {isSelected ? (
                <div className="w-full flex flex-col items-center justify-center gap-0.5 sm:gap-1 my-auto">
                  <span className="text-sm sm:text-base font-black text-stone-900">
                    {count} sỏi
                  </span>
                  <span className="text-[9px] sm:text-[10px] font-bold text-amber-950 uppercase">
                    Chọn hướng
                  </span>
                  <div className="flex items-center justify-center gap-1 w-full">
                    <button
                      type="button"
                      onClick={(e) => handleChooseDirection(e, 'LEFT')}
                      className="flex-1 py-1 px-1 bg-stone-900 text-stone-100 font-black text-[11px] sm:text-xs border border-stone-950 hover:bg-stone-800 cursor-pointer uppercase rounded-none"
                      aria-label="Rải sỏi sang trái"
                    >
                      ⬅ Trái
                    </button>
                    <button
                      type="button"
                      onClick={(e) => handleChooseDirection(e, 'RIGHT')}
                      className="flex-1 py-1 px-1 bg-stone-900 text-stone-100 font-black text-[11px] sm:text-xs border border-stone-950 hover:bg-stone-800 cursor-pointer uppercase rounded-none"
                      aria-label="Rải sỏi sang phải"
                    >
                      Phải ➡
                    </button>
                  </div>
                </div>
              ) : (
                <div className="my-auto flex flex-col items-center">
                  <span className="text-xl sm:text-2xl font-black text-stone-900">
                    {count}
                  </span>
                  <span className="text-[9px] sm:text-[11px] text-stone-600 font-semibold">
                    sỏi
                  </span>
                </div>
              )}

              <div className="h-2 flex items-center justify-center">
                {isClickable && !isSelected && (
                  <span className="text-[9px] sm:text-[10px] text-stone-700 font-bold">
                    • Chọn •
                  </span>
                )}
              </div>
            </div>
          );
        })}

        {/* 3. Ô Quan Phải (Position: 6, 0) - Bo tròn bên phải */}
        <div
          className="col-start-7 row-start-1 row-span-2 rounded-r-full border-4 border-stone-800 bg-[#d8ccbc] flex flex-col items-center justify-center p-2 text-center"
          aria-label={`Ô Quan Phải - ${getCount(6, 0)} sỏi`}
        >
          <span className="text-[10px] sm:text-xs font-black tracking-wider text-stone-700 uppercase">
            Quan Phải
          </span>
          <div className="my-1 flex items-center justify-center">
            <span className="text-2xl sm:text-3xl font-black text-stone-900">
              {getCount(6, 0)}
            </span>
          </div>
          <span className="text-[10px] sm:text-xs font-semibold text-stone-600">
            sỏi
          </span>
        </div>

        {/* 4. Hàng dưới: 5 ô dân của Player 1 (y = 1) - Viền dày, góc cạnh Anti-SaaS */}
        {p1Cells.map(({ x, y }, idx) => {
          const count = getCount(x, y);
          const isSelected = selectedPos?.x === x && selectedPos?.y === y;
          const isClickable = canSelectCell(x, y);

          return (
            <div
              key={`p1-${x}-${y}`}
              onClick={() => handleCellClick(x, y)}
              role="button"
              tabIndex={isClickable ? 0 : -1}
              aria-label={`Ô dân Player 1 cột ${x} - ${count} sỏi`}
              className={`col-start-${idx + 2} row-start-2 aspect-square rounded-none border-2 border-stone-800 p-1 sm:p-2 flex flex-col items-center justify-between transition-colors
                ${isSelected ? 'bg-amber-100 border-4 border-amber-800 ring-2 ring-amber-600' : 'bg-[#ded4c6]'}
                ${isClickable && !isSelected ? 'hover:bg-[#d4c8b8] cursor-pointer' : ''}
                ${!isClickable && !isSelected ? 'cursor-default' : ''}
              `}
            >
              <div className="w-full flex items-center justify-between text-[9px] sm:text-[11px] font-bold text-stone-600">
                <span>Dân {x}</span>
                <span className="text-red-700 font-extrabold">P1</span>
              </div>

              {/* Tương tác 2 bước: Nếu ô đang được chọn -> hiển thị số sỏi và 2 nút Trái / Phải */}
              {isSelected ? (
                <div className="w-full flex flex-col items-center justify-center gap-0.5 sm:gap-1 my-auto">
                  <span className="text-sm sm:text-base font-black text-stone-900">
                    {count} sỏi
                  </span>
                  <span className="text-[9px] sm:text-[10px] font-bold text-amber-950 uppercase">
                    Chọn hướng
                  </span>
                  <div className="flex items-center justify-center gap-1 w-full">
                    <button
                      type="button"
                      onClick={(e) => handleChooseDirection(e, 'LEFT')}
                      className="flex-1 py-1 px-1 bg-stone-900 text-stone-100 font-black text-[11px] sm:text-xs border border-stone-950 hover:bg-stone-800 cursor-pointer uppercase rounded-none"
                      aria-label="Rải sỏi sang trái"
                    >
                      ⬅ Trái
                    </button>
                    <button
                      type="button"
                      onClick={(e) => handleChooseDirection(e, 'RIGHT')}
                      className="flex-1 py-1 px-1 bg-stone-900 text-stone-100 font-black text-[11px] sm:text-xs border border-stone-950 hover:bg-stone-800 cursor-pointer uppercase rounded-none"
                      aria-label="Rải sỏi sang phải"
                    >
                      Phải ➡
                    </button>
                  </div>
                </div>
              ) : (
                <div className="my-auto flex flex-col items-center">
                  <span className="text-xl sm:text-2xl font-black text-stone-900">
                    {count}
                  </span>
                  <span className="text-[9px] sm:text-[11px] text-stone-600 font-semibold">
                    sỏi
                  </span>
                </div>
              )}

              <div className="h-2 flex items-center justify-center">
                {isClickable && !isSelected && (
                  <span className="text-[9px] sm:text-[10px] text-stone-700 font-bold">
                    • Chọn •
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
