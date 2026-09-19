package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OAnQuanRuleEngineTest {

    private OAnQuanRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OAnQuanRuleEngine();
    }

    @Test
    void testInitialBoardSetup() {
        BoardState board = OAnQuanRuleEngine.createInitialBoard();
        assertEquals(Player.PLAYER_1, board.currentPlayer());
        assertEquals(0, board.player1Score());
        assertEquals(0, board.player2Score());

        // 2 Quan = 10 each
        assertEquals(10, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[0]));
        assertEquals(10, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[6]));

        // 10 Dan = 5 each
        for (int i = 1; i <= 5; i++) {
            assertEquals(5, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[i]));
        }
        for (int i = 7; i <= 11; i++) {
            assertEquals(5, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[i]));
        }
    }

    @Test
    void testInitializeBoard_CallsCreateInitialBoard() {
        BoardState board = engine.initializeBoard();
        assertNotNull(board);
        assertEquals(Player.PLAYER_1, board.currentPlayer());
        assertEquals(0, board.player1Score());
        assertEquals(0, board.player2Score());
        assertEquals(10, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[0]));
        assertEquals(10, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[6]));
        for (int i = 1; i <= 5; i++) {
            assertEquals(5, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[i]));
        }
        for (int i = 7; i <= 11; i++) {
            assertEquals(5, board.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[i]));
        }
    }

    @Test
    void testIsValidMove_TurnValidation() {
        BoardState board = OAnQuanRuleEngine.createInitialBoard();
        MoveStep validP1Move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        assertTrue(engine.isValidMove(board, validP1Move, Player.PLAYER_1));
        assertFalse(engine.isValidMove(board, validP1Move, Player.PLAYER_2), "Player 2 cannot move on Player 1's turn");
    }

    @Test
    void testIsValidMove_CannotPickFromQuan() {
        BoardState board = OAnQuanRuleEngine.createInitialBoard();
        MoveStep pickQuan0 = new MoveStep(OAnQuanRuleEngine.POSITIONS[0], OAnQuanRuleEngine.POSITIONS[1]);
        MoveStep pickQuan6 = new MoveStep(OAnQuanRuleEngine.POSITIONS[6], OAnQuanRuleEngine.POSITIONS[7]);

        assertFalse(engine.isValidMove(board, pickQuan0, Player.PLAYER_1));
        assertFalse(engine.isValidMove(board, pickQuan6, Player.PLAYER_1));
    }

    @Test
    void testIsValidMove_CannotPickOpponentCell() {
        BoardState board = OAnQuanRuleEngine.createInitialBoard();
        MoveStep pickP2Cell = new MoveStep(OAnQuanRuleEngine.POSITIONS[7], OAnQuanRuleEngine.POSITIONS[8]);

        assertFalse(engine.isValidMove(board, pickP2Cell, Player.PLAYER_1), "Player 1 cannot pick Player 2's cell");
    }

    @Test
    void testIsValidMove_CannotPickEmptyCell() {
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        assertFalse(engine.isValidMove(board, move, Player.PLAYER_1));
    }

    @Test
    void testIsValidMove_NonAdjacentTargetFails() {
        BoardState board = OAnQuanRuleEngine.createInitialBoard();
        MoveStep jumpMove = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[3]);

        assertFalse(engine.isValidMove(board, jumpMove, Player.PLAYER_1));
    }

    @Test
    void testExecuteMove_LandedInEmpty_EatsNextCell() {
        // Setup:
        // P1 index 1 has 1 stone.
        // Index 2 is empty (0 stones).
        // Index 3 has 5 stones.
        // Move from index 1 to index 2:
        // Stone drops into index 2. Since index 2 was empty, it now has 1 stone.
        // Look at index 3: has 5 stones -> Player 1 eats index 3 (5 points)!
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[1], 1);
        counts.put(OAnQuanRuleEngine.POSITIONS[2], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[3], 5);

        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        BoardState result = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[1]));
        assertEquals(1, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[2]));
        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[3]), "Index 3 should be captured");
        assertEquals(5, result.player1Score(), "Player 1 should receive 5 points");
        assertEquals(Player.PLAYER_2, result.currentPlayer(), "Turn should swap to Player 2");
    }

    @Test
    void testExecuteMove_AnLienHoan() {
        // Setup:
        // Index 1: 1 stone -> spreads to index 2
        // Index 2: 0 stones (landed -> 1 stone)
        // Index 3: 4 stones -> captured!
        // Index 4: 0 stones (empty)
        // Index 5: 3 stones -> captured! (An lien hoan)
        // Index 6: 10 stones (not captured because index 5 was captured, next is 6 which has stones)
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[1], 1);
        counts.put(OAnQuanRuleEngine.POSITIONS[2], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[3], 4);
        counts.put(OAnQuanRuleEngine.POSITIONS[4], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[5], 3);
        counts.put(OAnQuanRuleEngine.POSITIONS[6], 10);

        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        BoardState result = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[1]));
        assertEquals(1, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[2]));
        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[3]));
        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[4]));
        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[5]));
        assertEquals(10, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[6]));
        assertEquals(7, result.player1Score(), "Player 1 captured 4 + 3 = 7 points");
    }

    @Test
    void testExecuteMove_ChocKhe_TurnEndsWithoutEating() {
        // Setup:
        // Index 1: 1 stone -> lands in index 2 (was 0)
        // Index 3: 0 stones -> empty cell next to it -> Choc khe, cannot eat!
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[1], 1);
        counts.put(OAnQuanRuleEngine.POSITIONS[2], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[3], 0);

        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        BoardState result = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(0, result.player1Score(), "No points captured on choc khe");
        assertEquals(Player.PLAYER_2, result.currentPlayer());
    }

    @Test
    void testExecuteMove_LandedInQuan_StopsImmediately() {
        // Setup:
        // Index 5: 1 stone -> spreads to index 6 (Right Quan)
        // Index 6: 10 stones -> turns into 11 stones
        // Even though index 6 is not empty, player cannot pick up Quan, turn ends
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[5], 1);
        counts.put(OAnQuanRuleEngine.POSITIONS[6], 10);
        counts.put(OAnQuanRuleEngine.POSITIONS[7], 5);

        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[5], OAnQuanRuleEngine.POSITIONS[6]);

        BoardState result = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[5]));
        assertEquals(11, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[6]));
        assertEquals(5, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[7]), "Index 7 should remain untouched");
        assertEquals(0, result.player1Score());
        assertEquals(Player.PLAYER_2, result.currentPlayer());
    }

    @Test
    void testExecuteMove_ContinuousSpreading() {
        // Setup:
        // Index 1: 2 stones
        // Index 2: 1 stone
        // Index 3: 0 stones
        // Index 4: 5 stones
        // Move from 1 to 2:
        // Stone 1 lands on 2 (index 2 now has 1 + 1 = 2 stones).
        // Stone 2 lands on 3 (index 3 now has 0 + 1 = 1 stone).
        // Since index 3 had 0 initially, last stone lands on empty cell 3!
        // Next is index 4 (has 5 stones) -> captured!
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[1], 2);
        counts.put(OAnQuanRuleEngine.POSITIONS[2], 1);
        counts.put(OAnQuanRuleEngine.POSITIONS[3], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[4], 5);

        BoardState board = new BoardState(Map.of(), counts, 0, 0, Player.PLAYER_1);
        MoveStep move = new MoveStep(OAnQuanRuleEngine.POSITIONS[1], OAnQuanRuleEngine.POSITIONS[2]);

        BoardState result = engine.executeMove(board, move, Player.PLAYER_1);

        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[1]));
        assertEquals(2, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[2]));
        assertEquals(1, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[3]));
        assertEquals(0, result.pieceCounts().get(OAnQuanRuleEngine.POSITIONS[4]), "Index 4 captured");
        assertEquals(5, result.player1Score());
    }

    @Test
    void testCheckWinner_BothQuanEmpty_P1Wins() {
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        // Quan empty
        counts.put(OAnQuanRuleEngine.POSITIONS[0], 0);
        counts.put(OAnQuanRuleEngine.POSITIONS[6], 0);

        // Remaining Dan
        counts.put(OAnQuanRuleEngine.POSITIONS[1], 2); // P1 gets +2
        counts.put(OAnQuanRuleEngine.POSITIONS[7], 1); // P2 gets +1

        BoardState board = new BoardState(Map.of(), counts, 30, 20, Player.PLAYER_1);

        Optional<Player> winner = engine.checkWinner(board, null);

        assertTrue(winner.isPresent());
        assertEquals(Player.PLAYER_1, winner.get());
    }

    @Test
    void testCheckWinner_QuanNotEmpty_ReturnsEmpty() {
        Map<Position, Integer> counts = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(OAnQuanRuleEngine.POSITIONS[i], 0);
        }
        counts.put(OAnQuanRuleEngine.POSITIONS[0], 10);
        counts.put(OAnQuanRuleEngine.POSITIONS[6], 0);

        BoardState board = new BoardState(Map.of(), counts, 30, 20, Player.PLAYER_1);

        assertEquals(Optional.empty(), engine.checkWinner(board, null));
    }
}
