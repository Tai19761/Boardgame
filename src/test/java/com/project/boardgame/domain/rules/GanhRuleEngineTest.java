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
}
