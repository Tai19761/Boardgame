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

class GanhRuleEngineTest {

    private GanhRuleEngine ruleEngine;
    private Map<Position, Piece> pieces;

    @BeforeEach
    void setUp() {
        ruleEngine = new GanhRuleEngine();
        pieces = new HashMap<>();
    }

    @Test
    void shouldAllowValidMove() {
        // Arrange
        Position from = new Position(2, 2);
        Position to = new Position(2, 3);
        pieces.put(from, new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act & Assert
        assertTrue(ruleEngine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void shouldRejectInvalidPathMove() {
        // Arrange
        Position from = new Position(1, 0); // Odd sum (1), no diagonal paths
        Position to = new Position(2, 1);   // Attempting diagonal
        pieces.put(from, new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act & Assert
        assertFalse(ruleEngine.isValidMove(board, move, Player.PLAYER_1), "Should not allow diagonal move on odd positions");
        
        // Also test moving too far
        MoveStep farMove = new MoveStep(new Position(1, 0), new Position(3, 0));
        assertFalse(ruleEngine.isValidMove(board, farMove, Player.PLAYER_1), "Should not allow moving more than 1 step");
    }

    @Test
    void shouldRejectMoveToOccupiedCell() {
        // Arrange
        Position from = new Position(2, 2);
        Position to = new Position(2, 3);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(to, new Piece(Player.PLAYER_2)); // Occupied by opponent
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act & Assert
        assertFalse(ruleEngine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void shouldApplyHorizontalGanh() {
        // Arrange
        Position from = new Position(2, 1);
        Position to = new Position(2, 2);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 2), new Piece(Player.PLAYER_2));
        pieces.put(new Position(3, 2), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertTrue(newBoard.pieces().containsKey(to));
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(to).player());
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(1, 2)).player(), "Left piece should be ganh-ed");
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(3, 2)).player(), "Right piece should be ganh-ed");
    }

    @Test
    void shouldApplyVerticalGanh() {
        // Arrange
        Position from = new Position(1, 2);
        Position to = new Position(2, 2);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(2, 1), new Piece(Player.PLAYER_2));
        pieces.put(new Position(2, 3), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(2, 1)).player(), "Top piece should be ganh-ed");
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(2, 3)).player(), "Bottom piece should be ganh-ed");
    }

    @Test
    void shouldApplyDiagonalGanh() {
        // Arrange
        Position from = new Position(1, 1);
        Position to = new Position(2, 2);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 3), new Piece(Player.PLAYER_2));
        pieces.put(new Position(3, 1), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(1, 3)).player(), "Top-right piece should be ganh-ed");
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(3, 1)).player(), "Bottom-left piece should be ganh-ed");
    }

    @Test
    void shouldNotApplyGanhIfOnlyOneOpponent() {
        // Arrange
        Position from = new Position(2, 1);
        Position to = new Position(2, 2);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 2), new Piece(Player.PLAYER_2));
        // No opponent piece on (3, 2)
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_2, newBoard.pieces().get(new Position(1, 2)).player(), "Left piece should NOT be ganh-ed");
    }

    @Test
    void shouldApplyChetSinglePiece() {
        // Arrange
        // Opponent at (0, 0), Player 1 moves to (1, 0).
        // The (0, 0) piece is in the corner, adjacent to (1, 0), (0, 1), and (1, 1).
        // Let's block (0, 1) and (1, 1) with Player 1 pieces.
        Position from = new Position(2, 0);
        Position to = new Position(1, 0);
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 1), new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 1), new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_2)); // To be chet
        
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(0, 0)).player(), "Corner piece should be chet-ed");
    }

    @Test
    void shouldApplyChetGroup() {
        // Arrange
        // Group of two P2 pieces at (0, 0) and (0, 1).
        // Surrounded by P1 pieces.
        Position from = new Position(2, 0);
        Position to = new Position(1, 0); // P1 moves here to close the group
        pieces.put(from, new Piece(Player.PLAYER_1));
        
        pieces.put(new Position(1, 1), new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 2), new Piece(Player.PLAYER_1));
        
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_2));
        pieces.put(new Position(0, 1), new Piece(Player.PLAYER_2));
        
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(0, 0)).player(), "Group piece 1 should be chet-ed");
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(new Position(0, 1)).player(), "Group piece 2 should be chet-ed");
    }

    @Test
    void shouldAllowMoWithoutBeingCaptured() {
        // Arrange
        // Player 1 moves to a position where they have 0 liberties.
        // It's a suicide move (Mở), so they should NOT be captured.
        Position from = new Position(0, 2);
        Position to = new Position(0, 1); // Move into a corner-like trap
        
        pieces.put(from, new Piece(Player.PLAYER_1));
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_2));
        pieces.put(new Position(1, 0), new Piece(Player.PLAYER_2));
        pieces.put(new Position(1, 1), new Piece(Player.PLAYER_2));
        
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_1, newBoard.pieces().get(to).player(), "Player piece should not be captured by suicide (Mở)");
    }

    @Test
    void shouldEnforceMandatoryGanhMove() {
        // Arrange
        // P1 has a piece at (2,1) that can move to (2,2) to Gánh P2 at (1,2) and (3,2).
        // P1 also has a piece at (0,0) that can move to (1,0) (not a Gánh).
        Position ganhFrom = new Position(2, 1);
        Position ganhTo = new Position(2, 2);
        Position normalFrom = new Position(0, 0);
        Position normalTo = new Position(1, 0);
        
        pieces.put(ganhFrom, new Piece(Player.PLAYER_1));
        pieces.put(normalFrom, new Piece(Player.PLAYER_1));
        
        pieces.put(new Position(1, 2), new Piece(Player.PLAYER_2));
        pieces.put(new Position(3, 2), new Piece(Player.PLAYER_2));
        
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        
        MoveStep ganhMove = new MoveStep(ganhFrom, ganhTo);
        MoveStep normalMove = new MoveStep(normalFrom, normalTo);

        // Act & Assert
        assertTrue(ruleEngine.isValidMove(board, ganhMove, Player.PLAYER_1), "Ganh move should be valid");
        assertFalse(ruleEngine.isValidMove(board, normalMove, Player.PLAYER_1), "Normal move should be invalid due to mandatory Ganh");
    }

    @Test
    void shouldRejectMoveOnWrongTurn() {
        Position from = new Position(2, 2);
        Position to = new Position(2, 3);
        pieces.put(from, new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_2);
        MoveStep move = new MoveStep(from, to);
        assertFalse(ruleEngine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void shouldSwapPlayerAfterExecuteMove() {
        Position from = new Position(2, 2);
        Position to = new Position(2, 3);
        pieces.put(from, new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        MoveStep move = new MoveStep(from, to);
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);
        assertEquals(Player.PLAYER_2, newBoard.currentPlayer());
    }

    @Test
    void testCheckWinner_Player1HasNoPieces_ReturnsPlayer2() {
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        assertEquals(java.util.Optional.of(Player.PLAYER_2), ruleEngine.checkWinner(board, null));
    }

    @Test
    void testCheckWinner_Player2HasNoPieces_ReturnsPlayer1() {
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        assertEquals(java.util.Optional.of(Player.PLAYER_1), ruleEngine.checkWinner(board, null));
    }

    @Test
    void testCheckWinner_BothHavePieces_ReturnsEmpty() {
        pieces.put(new Position(0, 0), new Piece(Player.PLAYER_1));
        pieces.put(new Position(1, 1), new Piece(Player.PLAYER_2));
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        assertEquals(java.util.Optional.empty(), ruleEngine.checkWinner(board, null));
    }

    @Test
    void testCheckWinner_EmptyBoard_ReturnsEmpty() {
        BoardState board = new BoardState(pieces, Player.PLAYER_1);
        assertEquals(java.util.Optional.empty(), ruleEngine.checkWinner(board, null));
    }

    @Test
    void testInitializeBoard() {
        BoardState initialBoard = ruleEngine.initializeBoard();
        assertNotNull(initialBoard);
        assertEquals(Player.PLAYER_1, initialBoard.currentPlayer());

        Map<Position, Piece> pieceMap = initialBoard.pieces();
        assertEquals(16, pieceMap.size(), "Initial Ganh board must have exactly 16 pieces");

        // Player 1 positions (8 pieces)
        Position[] p1Positions = new Position[] {
            new Position(0, 0), new Position(1, 0), new Position(2, 0), new Position(3, 0), new Position(4, 0),
            new Position(0, 1), new Position(4, 1),
            new Position(0, 2)
        };
        for (Position pos : p1Positions) {
            Piece piece = pieceMap.get(pos);
            assertNotNull(piece, "Expected piece at " + pos);
            assertEquals(Player.PLAYER_1, piece.player(), "Expected PLAYER_1 at " + pos);
        }

        // Player 2 positions (8 pieces)
        Position[] p2Positions = new Position[] {
            new Position(0, 4), new Position(1, 4), new Position(2, 4), new Position(3, 4), new Position(4, 4),
            new Position(0, 3), new Position(4, 3),
            new Position(4, 2)
        };
        for (Position pos : p2Positions) {
            Piece piece = pieceMap.get(pos);
            assertNotNull(piece, "Expected piece at " + pos);
            assertEquals(Player.PLAYER_2, piece.player(), "Expected PLAYER_2 at " + pos);
        }

        // Empty positions (9 cells)
        Position[] emptyPositions = new Position[] {
            new Position(1, 1), new Position(2, 1), new Position(3, 1),
            new Position(1, 2), new Position(2, 2), new Position(3, 2),
            new Position(1, 3), new Position(2, 3), new Position(3, 3)
        };
        for (Position pos : emptyPositions) {
            assertNull(pieceMap.get(pos), "Expected empty cell at " + pos);
        }
    }
}
