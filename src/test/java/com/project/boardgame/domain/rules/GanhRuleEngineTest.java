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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
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
        BoardState board = new BoardState(pieces);
        MoveStep move = new MoveStep(from, to);

        // Act
        BoardState newBoard = ruleEngine.executeMove(board, move, Player.PLAYER_1);

        // Assert
        assertEquals(Player.PLAYER_2, newBoard.pieces().get(new Position(1, 2)).player(), "Left piece should NOT be ganh-ed");
    }
}
