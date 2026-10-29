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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Engine AI cho Cờ Caro hỗ trợ 4 cấp độ khó:
 * - EASY (Depth 1, ngẫu nhiên có kiểm soát, luôn chặn/đánh nước thắng ngay).
 * - NORMAL (Depth 3, Minimax + Alpha-Beta chuẩn).
 * - HARD (Depth 4, Minimax + Alpha-Beta kết hợp Move Ordering).
 * - EXPERT (Depth 5, Minimax + Alpha-Beta kết hợp Zobrist Hashing và Transposition Table).
 * <p>
 * Luật cờ (sinh trạng thái con, kiểm tra hợp lệ, kiểm tra thắng) được ỦY QUYỀN hoàn toàn cho
 * {@link GameRuleEngine} được inject qua constructor — class này KHÔNG tự khởi tạo rule engine.
 */
public class CaroMinimax {

    public static final int DEFAULT_DEPTH = 3;
    public static final int SEARCH_RADIUS = 2;
    static final int WIN_SCORE = 100_000_000;

    private final GameRuleEngine ruleEngine;
    private final CaroHeuristicEvaluator evaluator;
    private final int boardSize;
    private final int depth;
    private final AiDifficulty difficulty;

    /** Cấu hình mặc định: bàn 15x15, độ sâu {@value #DEFAULT_DEPTH}, độ khó NORMAL. */
    public CaroMinimax(GameRuleEngine ruleEngine) {
        this(ruleEngine, CaroRuleEngine.DEFAULT_BOARD_SIZE);
    }

    /** Bàn {@code boardSize}x{@code boardSize}, độ sâu mặc định, độ khó NORMAL. */
    public CaroMinimax(GameRuleEngine ruleEngine, int boardSize) {
        this(ruleEngine, boardSize, AiDifficulty.NORMAL);
    }

    /** Bàn {@code boardSize}x{@code boardSize} theo cấp độ khó {@link AiDifficulty}. */
    public CaroMinimax(GameRuleEngine ruleEngine, int boardSize, AiDifficulty difficulty) {
        this(
                ruleEngine,
                new CaroHeuristicEvaluator(boardSize),
                boardSize,
                resolveDepth(difficulty != null ? difficulty : AiDifficulty.NORMAL),
                difficulty != null ? difficulty : AiDifficulty.NORMAL
        );
    }

    public CaroMinimax(GameRuleEngine ruleEngine, CaroHeuristicEvaluator evaluator, int boardSize, int depth) {
        this(ruleEngine, evaluator, boardSize, depth, AiDifficulty.NORMAL);
    }

    public CaroMinimax(GameRuleEngine ruleEngine, CaroHeuristicEvaluator evaluator, int boardSize, int depth, AiDifficulty difficulty) {
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
        this.difficulty = difficulty != null ? difficulty : AiDifficulty.NORMAL;
    }

    public static int resolveDepth(AiDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case NORMAL -> 3;
            case HARD -> 4;
            case EXPERT -> 5;
        };
    }

    public static int resolveMaxInnerBranching(AiDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 10;
            case NORMAL -> 20;
            case HARD -> 16;
            case EXPERT -> 12;
        };
    }

    public AiDifficulty getDifficulty() {
        return difficulty;
    }

    public int getDepth() {
        return depth;
    }

    public int getBoardSize() {
        return boardSize;
    }

    /**
     * Tìm nước đi tốt nhất cho {@code botPlayer} trên {@code board}.
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

        // 1. Nước thắng ngay cho Bot: đánh luôn
        Map<Position, BoardState> children = new HashMap<>();
        for (Position candidate : candidates) {
            BoardState child = apply(board, candidate, botPlayer);
            if (ruleEngine.checkWinner(child, candidate).isPresent()) {
                return candidate;
            }
            children.put(candidate, child);
        }

        // 2. Chặn nước đối thủ có thể thắng ngay ở lượt kế tiếp (áp dụng cho mọi cấp độ)
        Player opponent = botPlayer == Player.PLAYER_1 ? Player.PLAYER_2 : Player.PLAYER_1;
        BoardState opponentTurnBoard = new BoardState(board.pieces(), opponent);
        for (Position candidate : candidates) {
            BoardState opponentMove = apply(opponentTurnBoard, candidate, opponent);
            if (ruleEngine.checkWinner(opponentMove, candidate).isPresent()) {
                return candidate;
            }
        }

        // 3. Cấp độ EASY: chọn ngẫu nhiên trong top 3 ứng viên tốt nhất
        if (difficulty == AiDifficulty.EASY) {
            int poolSize = Math.min(3, candidates.size());
            int pick = ThreadLocalRandom.current().nextInt(poolSize);
            return candidates.get(pick);
        }

        int maxBranching = resolveMaxInnerBranching(difficulty);

        // 4. Cấp độ EXPERT: Minimax + Alpha-Beta kết hợp Transposition Table (Zobrist Hashing)
        if (difficulty == AiDifficulty.EXPERT) {
            TranspositionTable tt = new TranspositionTable();
            long rootHash = ZobristKeyGenerator.computeHash(board);

            Position bestMove = candidates.get(0);
            int bestScore = Integer.MIN_VALUE;
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;

            for (Position candidate : candidates) {
                BoardState child = children.get(candidate);
                long childHash = ZobristKeyGenerator.updateMove(rootHash, candidate, botPlayer);
                int score = minimaxExpert(child, depth - 1, alpha, beta, botPlayer, candidate, childHash, tt, maxBranching);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = candidate;
                }
                alpha = Math.max(alpha, bestScore);
            }
            return bestMove;
        }

        // 5. Cấp độ NORMAL và HARD: Minimax Alpha-Beta chuẩn
        Position bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Position candidate : candidates) {
            int score = minimax(children.get(candidate), depth - 1, alpha, beta, botPlayer, candidate, maxBranching);
            if (score > bestScore) {
                bestScore = score;
                bestMove = candidate;
            }
            alpha = Math.max(alpha, bestScore);
        }
        return bestMove;
    }

    private int minimax(BoardState board, int remainingDepth, int alpha, int beta, Player botPlayer, Position lastMove, int maxBranching) {
        Optional<Player> winner = ruleEngine.checkWinner(board, lastMove);
        if (winner.isPresent()) {
            return winner.get() == botPlayer ? WIN_SCORE + remainingDepth : -WIN_SCORE - remainingDepth;
        }
        if (board.pieces().size() >= boardSize * boardSize) {
            return 0; // Hòa: bàn đầy
        }
        if (remainingDepth == 0) {
            return evaluator.evaluate(board, botPlayer);
        }

        Player mover = board.currentPlayer();
        List<Position> candidates = orderedCandidates(board, mover, maxBranching);
        if (candidates.isEmpty()) {
            return evaluator.evaluate(board, botPlayer);
        }

        if (mover == botPlayer) {
            int value = Integer.MIN_VALUE;
            for (Position candidate : candidates) {
                value = Math.max(value, minimax(apply(board, candidate, mover), remainingDepth - 1, alpha, beta, botPlayer, candidate, maxBranching));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break; // Cắt tỉa Beta
                }
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Position candidate : candidates) {
                value = Math.min(value, minimax(apply(board, candidate, mover), remainingDepth - 1, alpha, beta, botPlayer, candidate, maxBranching));
                beta = Math.min(beta, value);
                if (alpha >= beta) {
                    break; // Cắt tỉa Alpha
                }
            }
            return value;
        }
    }

    private int minimaxExpert(
            BoardState board,
            int remainingDepth,
            int alpha,
            int beta,
            Player botPlayer,
            Position lastMove,
            long currentHash,
            TranspositionTable tt,
            int maxBranching
    ) {
        Optional<Player> winner = ruleEngine.checkWinner(board, lastMove);
        if (winner.isPresent()) {
            // Không lưu các trạng thái thắng/thua tuyệt đối vào TT để tránh lệch điểm theo depth giữa các nhánh
            return winner.get() == botPlayer ? WIN_SCORE + remainingDepth : -WIN_SCORE - remainingDepth;
        }
        if (board.pieces().size() >= boardSize * boardSize) {
            return 0;
        }
        if (remainingDepth == 0) {
            return evaluator.evaluate(board, botPlayer);
        }

        // Tra cứu Transposition Table cho các trạng thái trung gian
        Integer cachedScore = tt.lookup(currentHash, remainingDepth, alpha, beta);
        if (cachedScore != null) {
            return cachedScore;
        }

        int originalAlpha = alpha;
        Player mover = board.currentPlayer();
        List<Position> candidates = orderedCandidates(board, mover, maxBranching);
        if (candidates.isEmpty()) {
            return evaluator.evaluate(board, botPlayer);
        }

        int value;
        if (mover == botPlayer) {
            value = Integer.MIN_VALUE;
            for (Position candidate : candidates) {
                BoardState nextBoard = apply(board, candidate, mover);
                long nextHash = ZobristKeyGenerator.updateMove(currentHash, candidate, mover);
                int score = minimaxExpert(nextBoard, remainingDepth - 1, alpha, beta, botPlayer, candidate, nextHash, tt, maxBranching);
                value = Math.max(value, score);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break;
                }
            }
        } else {
            value = Integer.MAX_VALUE;
            for (Position candidate : candidates) {
                BoardState nextBoard = apply(board, candidate, mover);
                long nextHash = ZobristKeyGenerator.updateMove(currentHash, candidate, mover);
                int score = minimaxExpert(nextBoard, remainingDepth - 1, alpha, beta, botPlayer, candidate, nextHash, tt, maxBranching);
                value = Math.min(value, score);
                beta = Math.min(beta, value);
                if (alpha >= beta) {
                    break;
                }
            }
        }

        // Ghi nhận vào Transposition Table
        TranspositionTable.Flag flag;
        if (value <= originalAlpha) {
            flag = TranspositionTable.Flag.UPPER_BOUND;
        } else if (value >= beta) {
            flag = TranspositionTable.Flag.LOWER_BOUND;
        } else {
            flag = TranspositionTable.Flag.EXACT;
        }
        tt.store(currentHash, remainingDepth, value, flag);

        return value;
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
