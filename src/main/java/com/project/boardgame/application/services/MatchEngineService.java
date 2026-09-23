package com.project.boardgame.application.services;

import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.rules.GameRuleEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MatchEngineService {

    private BoardState currentBoard;
    private GameRuleEngine engine;
    private List<MoveStep> moveHistory = new ArrayList<>();
    private MatchStatus matchStatus;

    public void startMatch(GameRuleEngine engine) {
        this.engine = engine;
        this.currentBoard = engine.initializeBoard();
        this.moveHistory = new ArrayList<>();
        this.matchStatus = MatchStatus.ONGOING;
    }

    public void executeMove(MoveStep move, Player player) {
        // TODO: Khi tích hợp WebSocket, 'player' phải được xác định từ session đã xác thực,
        // KHÔNG được nhận trực tiếp làm tham số từ client, để tránh giả mạo danh tính.

        // Bước 1 — kiểm tra lượt đi
        if (player != currentBoard.currentPlayer()) {
            throw new InvalidMoveException("Không phải lượt của " + player);
        }

        // Bước 2 — kiểm tra luật cờ qua GameRuleEngine
        if (!engine.isValidMove(currentBoard, move, player)) {
            throw new InvalidMoveException("Nước đi không hợp lệ theo luật cờ");
        }

        // Hợp lệ: thực hiện nước đi và cập nhật trạng thái
        BoardState newBoard = engine.executeMove(currentBoard, move, player);
        this.currentBoard = newBoard;
        this.moveHistory.add(move);

        if (engine.isGameOver(newBoard)) {
            Optional<Player> winner = engine.checkWinner(newBoard, move.to());
            if (winner.isPresent()) {
                this.matchStatus = (winner.get() == Player.PLAYER_1) ? MatchStatus.WIN_PLAYER_1 : MatchStatus.WIN_PLAYER_2;
            } else {
                this.matchStatus = MatchStatus.DRAW;
            }
        } else {
            this.matchStatus = MatchStatus.ONGOING;
        }
    }

    public BoardState getCurrentBoard() {
        return currentBoard;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public List<MoveStep> getMoveHistory() {
        return Collections.unmodifiableList(new ArrayList<>(moveHistory));
    }
}
