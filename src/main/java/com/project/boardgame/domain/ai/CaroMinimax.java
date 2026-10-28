package com.project.boardgame.domain.ai;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import com.project.boardgame.domain.rules.GameRuleEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Engine AI cho Cờ Caro: Minimax + cắt tỉa Alpha-Beta (Java thuần, không phụ thuộc framework).
 * <p>
 * Luật cờ (sinh trạng thái con, kiểm tra hợp lệ, kiểm tra thắng) được ỦY QUYỀN hoàn toàn cho
 * {@link GameRuleEngine} được inject qua constructor — class này KHÔNG tự khởi tạo rule engine.
 * <p>
 * Tối ưu không gian tìm kiếm: chỉ xét các ô trống nằm trong bán kính {@value #SEARCH_RADIUS}
 * quanh các quân đã có trên bàn; ứng viên được sắp xếp theo heuristic để Alpha-Beta cắt tỉa sớm.
 */
public class CaroMinimax {

    public static final int DEFAULT_DEPTH = 3;
    public static final int SEARCH_RADIUS = 2;
    /** Giới hạn số nhánh ở các node bên trong (sau khi đã sắp xếp), node gốc luôn duyệt đủ. */
    static final int MAX_INNER_BRANCHING = 20;
    static final int WIN_SCORE = 100_000_000;

    private final GameRuleEngine ruleEngine;
    private final CaroHeuristicEvaluator evaluator;
    private final int boardSize;
    private final int depth;

    /** Cấu hình mặc định: bàn 15x15, độ sâu {@value #DEFAULT_DEPTH}. */
    public CaroMinimax(GameRuleEngine ruleEngine) {
        this(ruleEngine, CaroRuleEngine.DEFAULT_BOARD_SIZE);
    }

    /** Bàn {@code boardSize}x{@code boardSize}, độ sâu {@value #DEFAULT_DEPTH}; evaluator khởi tạo đúng kích thước. */
    public CaroMinimax(GameRuleEngine ruleEngine, int boardSize) {
        this(ruleEngine, new CaroHeuristicEvaluator(boardSize), boardSize, DEFAULT_DEPTH);
    }

    public CaroMinimax(GameRuleEngine ruleEngine, CaroHeuristicEvaluator evaluator, int boardSize, int depth) {
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        if (boardSize <= 0) {
            throw new IllegalArgumentException("boardSize phải > 0");
        }
        if (depth < 1) {
            throw new IllegalArgumentException("depth phải >= 1");
        }
        this.boardSize = boardSize;
        this.depth = depth;
    }

    /**
     * Tìm nước đi tốt nhất cho {@code botPlayer} trên {@code board}.
     * Bàn trống → đánh ô giữa. Có nước thắng ngay → đánh luôn. Còn lại → Minimax + Alpha-Beta.
     */
    public Position findBestMove(BoardState board, Player botPlayer) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(botPlayer, "botPlayer");

        if (board.pieces().isEmpty()) {
            return new Position(boardSize / 2, boardSize / 2);
        }

        List<Position> candidates = orderedCandidates(board, botPlayer, Integer.MAX_VALUE);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Không còn nước đi hợp lệ cho Bot");
        }

        // Sinh trạng thái con 1 lần cho node gốc; thắng ngay thì không cần tìm kiếm sâu hơn
        Map<Position, BoardState> children = new HashMap<>();
        for (Position candidate : candidates) {
            BoardState child = apply(board, candidate, botPlayer);
            if (ruleEngine.checkWinner(child, candidate).isPresent()) {
                return candidate;
            }
            children.put(candidate, child);
        }

        Position bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Position candidate : candidates) {
            int score = minimax(children.get(candidate), depth - 1, alpha, beta, botPlayer, candidate);
            if (score > bestScore) {
                bestScore = score;
                bestMove = candidate;
            }
            alpha = Math.max(alpha, bestScore);
        }
        return bestMove;
    }

    private int minimax(BoardState board, int remainingDepth, int alpha, int beta, Player botPlayer, Position lastMove) {
        // Kiểm tra kết thúc dựa trên nước vừa đi (O(1) theo 4 hướng thay vì quét toàn bàn)
        Optional<Player> winner = ruleEngine.checkWinner(board, lastMove);
        if (winner.isPresent()) {
            // Thắng càng sớm (remainingDepth lớn) càng tốt, thua càng muộn càng đỡ
            return winner.get() == botPlayer ? WIN_SCORE + remainingDepth : -WIN_SCORE - remainingDepth;
        }
        if (board.pieces().size() >= boardSize * boardSize) {
            return 0; // Hòa: bàn đầy
        }
        if (remainingDepth == 0) {
            return evaluator.evaluate(board, botPlayer);
        }

        Player mover = board.currentPlayer();
        List<Position> candidates = orderedCandidates(board, mover, MAX_INNER_BRANCHING);
        if (candidates.isEmpty()) {
            return evaluator.evaluate(board, botPlayer);
        }

        if (mover == botPlayer) {
            int value = Integer.MIN_VALUE;
            for (Position candidate : candidates) {
                value = Math.max(value, minimax(apply(board, candidate, mover), remainingDepth - 1, alpha, beta, botPlayer, candidate));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break; // Cắt tỉa Beta
                }
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Position candidate : candidates) {
                value = Math.min(value, minimax(apply(board, candidate, mover), remainingDepth - 1, alpha, beta, botPlayer, candidate));
                beta = Math.min(beta, value);
                if (alpha >= beta) {
                    break; // Cắt tỉa Alpha
                }
            }
            return value;
        }
    }

    /**
     * Sinh danh sách ô ứng viên: ô trống, trong bàn, hợp luật (theo GameRuleEngine), nằm trong
     * bán kính {@value #SEARCH_RADIUS} (khoảng cách Chebyshev) quanh ít nhất một quân đã có.
     * Kết quả được sắp xếp xác định theo (y, x).
     */
    public List<Position> generateCandidates(BoardState board) {
        Set<Position> result = new LinkedHashSet<>();
        Player mover = board.currentPlayer();
        for (Position occupied : board.pieces().keySet()) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    Position cell = new Position(occupied.x() + dx, occupied.y() + dy);
                    if (!isWithinBounds(cell) || board.pieces().containsKey(cell) || result.contains(cell)) {
                        continue;
                    }
                    if (ruleEngine.isValidMove(board, new MoveStep(null, cell), mover)) {
                        result.add(cell);
                    }
                }
            }
        }
        List<Position> sorted = new ArrayList<>(result);
        sorted.sort(Comparator.comparingInt(Position::y).thenComparingInt(Position::x));
        return sorted;
    }

    private List<Position> orderedCandidates(BoardState board, Player mover, int limit) {
        List<Position> candidates = generateCandidates(board);
        Map<Position, Integer> scores = new HashMap<>();
        for (Position c : candidates) {
            scores.put(c, evaluator.quickMoveScore(board, c, mover));
        }
        // Sort ổn định: cùng điểm thì giữ thứ tự (y, x) → kết quả xác định giữa các lần chạy
        candidates.sort(Comparator.comparingInt((Position c) -> scores.get(c)).reversed());
        return candidates.size() > limit ? new ArrayList<>(candidates.subList(0, limit)) : candidates;
    }

    private BoardState apply(BoardState board, Position cell, Player mover) {
        return ruleEngine.executeMove(board, new MoveStep(null, cell), mover);
    }

    private boolean isWithinBounds(Position p) {
        return p.x() >= 0 && p.x() < boardSize && p.y() >= 0 && p.y() < boardSize;
    }
}
