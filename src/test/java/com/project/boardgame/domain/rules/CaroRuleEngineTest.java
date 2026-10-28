package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaroRuleEngineTest {

    private CaroRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CaroRuleEngine();
    }

    @Test
    void testIsValidMove_EmptyCell_ReturnsTrue() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(null, new Position(0, 0));

        assertTrue(engine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void testIsValidMove_OccupiedCell_ReturnsFalse() {
        Map<Position, Piece> pieces = new HashMap<>();
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_2);
        MoveStep move = new MoveStep(null, new Position(0, 0));

        assertFalse(engine.isValidMove(board, move, Player.PLAYER_2));
    }

    @Test
    void testIsValidMove_OutOfBounds_Negative_ReturnsFalse() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(null, new Position(-1, 5));

        assertFalse(engine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void testIsValidMove_OutOfBounds_GreaterThan14_ReturnsFalse() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(null, new Position(5, 15));

        assertFalse(engine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void testExecuteMove_ValidMove_UpdatesBoard() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(null, new Position(5, 5));

        BoardState newBoard = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(1, newBoard.pieces().size());
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(5, 5)).player());
    }

    @Test
    void testExecuteMove_InvalidMove_ThrowsException() {
        Map<Position, Piece> pieces = new HashMap<>();
        pieces.put(new Position(2, 2), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_2);
        MoveStep move = new MoveStep(null, new Position(2, 2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            engine.executeMove(board, move, Player.PLAYER_2);
        });

        assertEquals("Invalid move: Cell is not empty", exception.getMessage());
    }

    @Test
    void testCheckWinCondition_HorizontalWin_ReturnsTrue() {
        Map<Position, Piece> pieces = new HashMap<>();
        // 5 quân ngang
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(2, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(3, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(4, 0), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);

        assertEquals(java.util.Optional.of(Player.PLAYER_1), engine.checkWinner(board, new Position(2, 0)));
    }

    @Test
    void testCheckWinCondition_VerticalWin_ReturnsTrue() {
        Map<Position, Piece> pieces = new HashMap<>();
        // 5 quân dọc
        pieces.put(new Position(5, 1), new Piece(Player.PLAYER_2));
        pieces.put(new Position(5, 2), new Piece(Player.PLAYER_2));
        pieces.put(new Position(5, 3), new Piece(Player.PLAYER_2));
        pieces.put(new Position(5, 4), new Piece(Player.PLAYER_2));
        pieces.put(new Position(5, 5), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_2);

        assertEquals(java.util.Optional.of(Player.PLAYER_2), engine.checkWinner(board, new Position(5, 5)));
    }

    @Test
    void testCheckWinCondition_DiagonalWin_Main_ReturnsTrue() {
        Map<Position, Piece> pieces = new HashMap<>();
        // 5 quân chéo chính \
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 1), new Piece(Player.PLAYER_1));
        pieces.put(new Position(2, 2), new Piece(Player.PLAYER_1));
        pieces.put(new Position(3, 3), new Piece(Player.PLAYER_1));
        pieces.put(new Position(4, 4), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);

        assertEquals(java.util.Optional.of(Player.PLAYER_1), engine.checkWinner(board, new Position(3, 3)));
    }

    @Test
    void testCheckWinCondition_DiagonalWin_Sub_ReturnsTrue() {
        Map<Position, Piece> pieces = new HashMap<>();
        // 5 quân chéo phụ /
        pieces.put(new Position(5, 1), new Piece(Player.PLAYER_2));
        pieces.put(new Position(4, 2), new Piece(Player.PLAYER_2));
        pieces.put(new Position(3, 3), new Piece(Player.PLAYER_2));
        pieces.put(new Position(2, 4), new Piece(Player.PLAYER_2));
        pieces.put(new Position(1, 5), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_2);

        assertEquals(java.util.Optional.of(Player.PLAYER_2), engine.checkWinner(board, new Position(2, 4)));
    }

    @Test
    void testCheckWinCondition_LessThan5_ReturnsFalse() {
        Map<Position, Piece> pieces = new HashMap<>();
        // 4 quân dọc
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 1), new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 2), new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 3), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);

        assertEquals(java.util.Optional.empty(), engine.checkWinner(board, new Position(0, 3)));
    }

    @Test
    void testIsValidMove_WrongTurn_ReturnsFalse() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_2);
        MoveStep move = new MoveStep(null, new Position(0, 0));
        assertFalse(engine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void testExecuteMove_ValidMove_SwapsPlayer() {
        Map<Position, Piece> pieces = new HashMap<>();
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(null, new Position(0, 0));
        BoardState newBoard = engine.executeMove(board, move, Player.PLAYER_1);
        assertEquals(Player.PLAYER_2, newBoard.currentPlayer());
    }

    @Test
    void testInitializeBoard() {
        BoardState initialBoard = engine.initializeBoard();
        assertNotNull(initialBoard);
        assertTrue(initialBoard.pieces().isEmpty(), "Initial Caro board should be empty");
        assertEquals(Player.PLAYER_1, initialBoard.currentPlayer(), "Player 1 starts first");
    }

    @Test
    void testConstructor_GuardClause_ThrowsWhenSizeLessThan5() {
        assertThrows(IllegalArgumentException.class, () -> new CaroRuleEngine(4));
        assertThrows(IllegalArgumentException.class, () -> new CaroRuleEngine(0));
        assertThrows(IllegalArgumentException.class, () -> new CaroRuleEngine(-1));
        assertDoesNotThrow(() -> new CaroRuleEngine(5));
    }

    @Test
    void testDynamicBoardSize_Size20_BoundaryChecks() {
        CaroRuleEngine engine20 = new CaroRuleEngine(20);
        assertEquals(20, engine20.getBoardSize());

        BoardState emptyBoard = engine20.initializeBoard();

        // Ô (19, 19) hợp lệ
        MoveStep validMove = new MoveStep(null, new Position(19, 19));
        assertTrue(engine20.isValidMove(emptyBoard, validMove, Player.PLAYER_1));

        // Ô (20, 19) và (19, 20) vượt biên
        MoveStep outOfBounds1 = new MoveStep(null, new Position(20, 19));
        MoveStep outOfBounds2 = new MoveStep(null, new Position(19, 20));
        assertFalse(engine20.isValidMove(emptyBoard, outOfBounds1, Player.PLAYER_1));
        assertFalse(engine20.isValidMove(emptyBoard, outOfBounds2, Player.PLAYER_1));

        assertThrows(IllegalArgumentException.class, () -> {
            engine20.executeMove(emptyBoard, outOfBounds1, Player.PLAYER_1);
        });
    }

    @Test
    void testIsGameOver_BoardFull_CalculatedBasedOnSizeSquared() {
        // Sử dụng bàn cờ nhỏ 5x5 để kiểm tra tính đầy bàn = 25 ô
        CaroRuleEngine engine5 = new CaroRuleEngine(5);
        Map<Position, Piece> pieces = new HashMap<>();

        // Ma trận 5x5 không có bất kỳ hàng, cột hay đường chéo nào có 5 quân liên tiếp
        Player[][] grid = {
            {Player.PLAYER_1, Player.PLAYER_1, Player.PLAYER_2, Player.PLAYER_2, Player.PLAYER_1},
            {Player.PLAYER_2, Player.PLAYER_2, Player.PLAYER_1, Player.PLAYER_1, Player.PLAYER_2},
            {Player.PLAYER_1, Player.PLAYER_1, Player.PLAYER_2, Player.PLAYER_2, Player.PLAYER_1},
            {Player.PLAYER_2, Player.PLAYER_2, Player.PLAYER_1, Player.PLAYER_1, Player.PLAYER_2},
            {Player.PLAYER_1, Player.PLAYER_1, Player.PLAYER_2, Player.PLAYER_2, Player.PLAYER_1}
        };
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                pieces.put(new Position(x, y), new Piece(grid[y][x]));
            }
        }
        assertEquals(25, pieces.size());

        BoardState fullBoard = new BoardState(pieces, Player.PLAYER_1);
        assertTrue(engine5.isGameOver(fullBoard), "Bàn 5x5 có 25 quân cờ phải được xác định là kết thúc ván (hòa)");

        // Nếu chỉ có 24 ô thì chưa đầy bàn
        pieces.remove(new Position(4, 4));
        BoardState notFullBoard = new BoardState(pieces, Player.PLAYER_1);
        assertFalse(engine5.isGameOver(notFullBoard), "Bàn 5x5 mới có 24 quân và chưa có ai thắng thì chưa kết thúc");
    }
}
