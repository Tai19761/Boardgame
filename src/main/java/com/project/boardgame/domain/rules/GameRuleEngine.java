package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.Optional;

public interface GameRuleEngine {
    /**
     * Khởi tạo bàn cờ ban đầu theo quy chuẩn của từng loại trò chơi.
     */
    BoardState initializeBoard();

    boolean isValidMove(BoardState board, MoveStep move, Player player);
    BoardState executeMove(BoardState board, MoveStep move, Player player);

    /**
     * Kiểm tra trạng thái ván đấu đã kết thúc hay chưa.
     * Tầng điều phối cần gọi phương thức này trước:
     * - Nếu trả về false: Ván đấu đang tiếp diễn (ONGOING).
     * - Nếu trả về true: Ván đấu đã kết thúc (GAME OVER), sau đó mới gọi checkWinner để xác định WIN hay DRAW.
     */
    boolean isGameOver(BoardState board);

    /**
     * Kiểm tra người chơi chiến thắng.
     * <p>
     * QUY ƯỚC QUAN TRỌNG:
     * Kết quả Optional.empty() chỉ có ý nghĩa xác định là 'Hòa' (DRAW) khi isGameOver(board) đã trả về true.
     * Nếu isGameOver(board) == false, không được diễn giải Optional.empty() là kết quả cuối cùng — ván đấu vẫn đang tiếp diễn (ONGOING).
     */
    Optional<Player> checkWinner(BoardState board, Position lastMove);
}
