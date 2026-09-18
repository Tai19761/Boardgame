package com.project.boardgame.domain.rules;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OAnQuanRuleEngine implements GameRuleEngine {

    public static final int BOARD_SIZE = 12;
    public static final int QUAN_LEFT_INDEX = 0;
    public static final int QUAN_RIGHT_INDEX = 6;

    // Canonical 2D mapping for 12 cells
    // Index 0: Quan Trai (0,0)
    // Index 1..5: Dan Player 1 (1,1) -> (5,1)
    // Index 6: Quan Phai (6,0)
    // Index 7..11: Dan Player 2 (5,0) -> (1,0)
    public static final Position[] POSITIONS = new Position[] {
        new Position(0, 0),
        new Position(1, 1),
        new Position(2, 1),
        new Position(3, 1),
        new Position(4, 1),
        new Position(5, 1),
        new Position(6, 0),
        new Position(5, 0),
        new Position(4, 0),
        new Position(3, 0),
        new Position(2, 0),
        new Position(1, 0)
    };

    public static int positionToIndex(Position pos) {
        if (pos == null) {
            return -1;
        }
        for (int i = 0; i < POSITIONS.length; i++) {
            if (POSITIONS[i].equals(pos)) {
                return i;
            }
        }
        return -1;
    }

    public static Position indexToPosition(int index) {
        int idx = (index % BOARD_SIZE + BOARD_SIZE) % BOARD_SIZE;
        return POSITIONS[idx];
    }

    public static BoardState createInitialBoard() {
        Map<Position, Integer> pieceCounts = new HashMap<>();
        // 2 Quan: 10 stones each
        pieceCounts.put(POSITIONS[QUAN_LEFT_INDEX], 10);
        pieceCounts.put(POSITIONS[QUAN_RIGHT_INDEX], 10);
        // 10 Dan: 5 stones each
        for (int i = 1; i <= 5; i++) {
            pieceCounts.put(POSITIONS[i], 5);
        }
        for (int i = 7; i <= 11; i++) {
            pieceCounts.put(POSITIONS[i], 5);
        }
        return new BoardState(Map.of(), pieceCounts, 0, 0, Player.PLAYER_1);
    }

    @Override
    public boolean isValidMove(BoardState board, MoveStep move, Player player) {
        if (player != board.currentPlayer()) {
            return false;
        }
        if (move == null || move.from() == null || move.to() == null) {
            return false;
        }

        int fromIndex = positionToIndex(move.from());
        int toIndex = positionToIndex(move.to());

        if (fromIndex < 0 || toIndex < 0) {
            return false;
        }

        // Cannot pick from Quan cells
        if (fromIndex == QUAN_LEFT_INDEX || fromIndex == QUAN_RIGHT_INDEX) {
            return false;
        }

        // Player must own the starting cell
        if (player == Player.PLAYER_1) {
            if (fromIndex < 1 || fromIndex > 5) {
                return false;
            }
        } else if (player == Player.PLAYER_2) {
            if (fromIndex < 7 || fromIndex > 11) {
                return false;
            }
        } else {
            return false;
        }

        // Starting cell must have stones
        int count = getPieceCount(board, fromIndex);
        if (count <= 0) {
            return false;
        }

        // `to` must be adjacent to `from` in the 12-cell ring
        int forwardAdjacent = (fromIndex + 1) % BOARD_SIZE;
        int backwardAdjacent = (fromIndex + BOARD_SIZE - 1) % BOARD_SIZE;
        return toIndex == forwardAdjacent || toIndex == backwardAdjacent;
    }

    @Override
    public BoardState executeMove(BoardState board, MoveStep move, Player player) {
        int fromIndex = positionToIndex(move.from());
        int toIndex = positionToIndex(move.to());

        // Determine step direction (+1 for clockwise / forward, -1 for counter-clockwise / backward)
        int step = (toIndex == (fromIndex + 1) % BOARD_SIZE) ? 1 : -1;

        // Copy board counts into an array for fast indexed access
        int[] counts = new int[BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            counts[i] = getPieceCount(board, i);
        }

        int p1Score = board.player1Score();
        int p2Score = board.player2Score();

        // Pick up all stones from starting cell
        int stones = counts[fromIndex];
        counts[fromIndex] = 0;

        int currIndex = fromIndex;

        while (true) {
            // Distribute stones one by one
            while (stones > 0) {
                currIndex = (currIndex + step + BOARD_SIZE) % BOARD_SIZE;
                counts[currIndex]++;
                stones--;
            }

            // After dropping the last stone at currIndex:
            // Case 1: Landed in Quan cell -> Turn ends immediately (cannot pick up Quan)
            if (currIndex == QUAN_LEFT_INDEX || currIndex == QUAN_RIGHT_INDEX) {
                break;
            }

            // Case 2: Landed in a non-empty cell (now has > 1 stones) -> Pick up and continue
            if (counts[currIndex] > 1) {
                stones = counts[currIndex];
                counts[currIndex] = 0;
                continue;
            }

            // Case 3: Landed in an empty cell (now has exactly 1 stone) -> Check for captures
            int targetIndex = (currIndex + step + BOARD_SIZE) % BOARD_SIZE;

            while (true) {
                if (counts[targetIndex] > 0) {
                    // Capture stones at targetIndex (including Quan cell if it has stones)
                    int captured = counts[targetIndex];
                    counts[targetIndex] = 0;
                    if (player == Player.PLAYER_1) {
                        p1Score += captured;
                    } else {
                        p2Score += captured;
                    }

                    // Look at the cell after targetIndex for "ăn liên hoàn"
                    int emptyCellIndex = (targetIndex + step + BOARD_SIZE) % BOARD_SIZE;
                    if (counts[emptyCellIndex] == 0) {
                        // Empty cell followed by potential capture cell
                        targetIndex = (emptyCellIndex + step + BOARD_SIZE) % BOARD_SIZE;
                    } else {
                        // Not empty -> chain stops
                        break;
                    }
                } else {
                    // Next cell is empty -> cannot capture ("chọc khế"), turn ends
                    break;
                }
            }
            break;
        }

        // Convert counts back to Map<Position, Integer>
        Map<Position, Integer> newPieceCounts = new HashMap<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            newPieceCounts.put(POSITIONS[i], counts[i]);
        }

        Player nextPlayer = (player == Player.PLAYER_1) ? Player.PLAYER_2 : Player.PLAYER_1;
        return new BoardState(Map.of(), newPieceCounts, p1Score, p2Score, nextPlayer);
    }

    @Override
    public boolean isGameOver(BoardState board) {
        int quanLeft = getPieceCount(board, QUAN_LEFT_INDEX);
        int quanRight = getPieceCount(board, QUAN_RIGHT_INDEX);
        return quanLeft == 0 && quanRight == 0;
    }

    @Override
    public Optional<Player> checkWinner(BoardState board, Position lastMove) {
        if (!isGameOver(board)) {
            // Kết quả Optional.empty() chỉ có ý nghĩa xác định là 'Hòa' khi isGameOver(board) == true.
            // Nếu isGameOver(board) == false, ván đấu vẫn đang tiếp diễn (ONGOING).
            return Optional.empty();
        }

        int finalP1Score = board.player1Score();
        int finalP2Score = board.player2Score();

        // Collect remaining Dan stones for each player
        for (int i = 1; i <= 5; i++) {
            finalP1Score += getPieceCount(board, i);
        }
        for (int i = 7; i <= 11; i++) {
            finalP2Score += getPieceCount(board, i);
        }

        if (finalP1Score > finalP2Score) {
            return Optional.of(Player.PLAYER_1);
        } else if (finalP2Score > finalP1Score) {
            return Optional.of(Player.PLAYER_2);
        } else {
            // Hòa: isGameOver() == true và điểm 2 bên bằng nhau
            return Optional.empty();
        }
    }

    private int getPieceCount(BoardState board, int index) {
        if (board.pieceCounts() != null) {
            Integer count = board.pieceCounts().get(POSITIONS[index]);
            if (count != null) {
                return count;
            }
        }
        return 0;
    }
}
