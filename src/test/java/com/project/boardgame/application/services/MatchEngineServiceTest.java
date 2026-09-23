package com.project.boardgame.application.services;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEngineServiceTest {

    private MatchEngineService service;
    private CaroRuleEngine caroRuleEngine;

    @BeforeEach
    void setUp() {
        service = new MatchEngineService();
        caroRuleEngine = new CaroRuleEngine();
        service.startMatch(caroRuleEngine);
    }

    @Test
    @DisplayName("Nước đi hợp lệ: cập nhật BoardState, đổi lượt, thêm vào lịch sử, trạng thái vẫn ONGOING")
    void testExecuteMove_ValidMove_UpdatesBoardTurnHistoryStatus() {
        MoveStep move = new MoveStep(null, new Position(7, 7));

        service.executeMove(move, Player.PLAYER_1);

        BoardState board = service.getCurrentBoard();
        assertNotNull(board);
        assertEquals(Player.PLAYER_2, board.currentPlayer(), "Lượt phải chuyển sang PLAYER_2");
        Piece placedPiece = board.pieces().get(new Position(7, 7));
        assertNotNull(placedPiece);
        assertEquals(Player.PLAYER_1, placedPiece.player());

        List<MoveStep> history = service.getMoveHistory();
        assertEquals(1, history.size());
        assertEquals(move, history.get(0));

        assertEquals(MatchStatus.ONGOING, service.getMatchStatus());
    }

    @Test
    @DisplayName("Đi sai lượt: ném InvalidMoveException, không đổi BoardState và không thêm vào lịch sử")
    void testExecuteMove_WrongTurn_ThrowsInvalidMoveExceptionAndDoesNotModifyState() {
        BoardState boardBefore = service.getCurrentBoard();
        MoveStep move = new MoveStep(null, new Position(7, 7));

        InvalidMoveException exception = assertThrows(
                InvalidMoveException.class,
                () -> service.executeMove(move, Player.PLAYER_2),
                "Phải ném InvalidMoveException khi PLAYER_2 đi vào lượt của PLAYER_1"
        );

        assertTrue(exception.getMessage().contains("Không phải lượt của PLAYER_2"));
        assertSame(boardBefore, service.getCurrentBoard(), "BoardState không được thay đổi");
        assertTrue(service.getMoveHistory().isEmpty(), "Lịch sử không được thêm nước đi sai");
        assertEquals(MatchStatus.ONGOING, service.getMatchStatus());
    }

    @Test
    @DisplayName("Đi đúng lượt nhưng sai luật cờ: ném InvalidMoveException, không đổi BoardState và không thêm vào lịch sử")
    void testExecuteMove_InvalidRuleMove_ThrowsInvalidMoveExceptionAndDoesNotModifyState() {
        MoveStep firstMove = new MoveStep(null, new Position(7, 7));
        service.executeMove(firstMove, Player.PLAYER_1);

        BoardState boardAfterFirstMove = service.getCurrentBoard();
        int historySizeAfterFirstMove = service.getMoveHistory().size();

        // PLAYER_2 đi đúng lượt nhưng chọn ô (7, 7) đã có quân của PLAYER_1
        MoveStep duplicateMove = new MoveStep(null, new Position(7, 7));

        InvalidMoveException exception = assertThrows(
                InvalidMoveException.class,
                () -> service.executeMove(duplicateMove, Player.PLAYER_2),
                "Phải ném InvalidMoveException khi đánh vào ô đã có quân"
        );

        assertEquals("Nước đi không hợp lệ theo luật cờ", exception.getMessage());
        assertSame(boardAfterFirstMove, service.getCurrentBoard(), "BoardState không được thay đổi khi nước đi phạm luật");
        assertEquals(historySizeAfterFirstMove, service.getMoveHistory().size(), "Lịch sử không được thêm nước đi phạm luật");
        assertEquals(Player.PLAYER_2, service.getCurrentBoard().currentPlayer(), "Vẫn giữ nguyên lượt của PLAYER_2");
        assertEquals(MatchStatus.ONGOING, service.getMatchStatus());
    }

    @Test
    @DisplayName("Luồng game over: PLAYER_1 thắng khi đủ 5 quân liên tiếp")
    void testExecuteMove_GameOver_WinPlayer1() {
        // P1: (0,0), P2: (0,1)
        service.executeMove(new MoveStep(null, new Position(0, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(0, 1)), Player.PLAYER_2);

        // P1: (1,0), P2: (1,1)
        service.executeMove(new MoveStep(null, new Position(1, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(1, 1)), Player.PLAYER_2);

        // P1: (2,0), P2: (2,1)
        service.executeMove(new MoveStep(null, new Position(2, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(2, 1)), Player.PLAYER_2);

        // P1: (3,0), P2: (3,1)
        service.executeMove(new MoveStep(null, new Position(3, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(3, 1)), Player.PLAYER_2);

        assertEquals(MatchStatus.ONGOING, service.getMatchStatus());

        // P1 đi nước thứ 5 tạo chuỗi 5: (4,0)
        service.executeMove(new MoveStep(null, new Position(4, 0)), Player.PLAYER_1);

        assertEquals(MatchStatus.WIN_PLAYER_1, service.getMatchStatus(), "MatchStatus phải là WIN_PLAYER_1");
        assertEquals(9, service.getMoveHistory().size());
    }

    @Test
    @DisplayName("Luồng game over: PLAYER_2 thắng khi đủ 5 quân liên tiếp")
    void testExecuteMove_GameOver_WinPlayer2() {
        // P1: (0,0), P2: (0,1)
        service.executeMove(new MoveStep(null, new Position(0, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(0, 1)), Player.PLAYER_2);

        // P1: (1,0), P2: (1,1)
        service.executeMove(new MoveStep(null, new Position(1, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(1, 1)), Player.PLAYER_2);

        // P1: (2,0), P2: (2,1)
        service.executeMove(new MoveStep(null, new Position(2, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(2, 1)), Player.PLAYER_2);

        // P1: (3,0), P2: (3,1)
        service.executeMove(new MoveStep(null, new Position(3, 0)), Player.PLAYER_1);
        service.executeMove(new MoveStep(null, new Position(3, 1)), Player.PLAYER_2);

        // P1 đánh nước khác: (10,10)
        service.executeMove(new MoveStep(null, new Position(10, 10)), Player.PLAYER_1);
        assertEquals(MatchStatus.ONGOING, service.getMatchStatus());

        // P2 đi nước thứ 5: (4,1)
        service.executeMove(new MoveStep(null, new Position(4, 1)), Player.PLAYER_2);

        assertEquals(MatchStatus.WIN_PLAYER_2, service.getMatchStatus(), "MatchStatus phải là WIN_PLAYER_2");
        assertEquals(10, service.getMoveHistory().size());
    }
}
