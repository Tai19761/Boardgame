package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.services.MatchEngineService;
import com.project.boardgame.application.services.MatchStatus;
import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;

import java.util.Map;

public record GameStateResponse(
        String roomId,
        MatchStatus matchStatus,
        Player currentPlayer,
        Map<Position, Piece> pieces,
        Map<Position, Integer> pieceCounts,
        int player1Score,
        int player2Score,
        Player winner
) {

    public static GameStateResponse from(String roomId, Room room) {
        MatchEngineService engine = room.getMatchEngine();
        BoardState board = engine.getCurrentBoard();
        MatchStatus status = engine.getMatchStatus();

        Player winner = switch (status) {
            case WIN_PLAYER_1 -> Player.PLAYER_1;
            case WIN_PLAYER_2 -> Player.PLAYER_2;
            default -> null;
        };

        if (board == null) {
            return new GameStateResponse(
                    roomId,
                    status != null ? status : MatchStatus.ONGOING,
                    null,
                    Map.of(),
                    Map.of(),
                    0,
                    0,
                    null
            );
        }

        return new GameStateResponse(
                roomId,
                status,
                board.currentPlayer(),
                board.pieces(),
                board.pieceCounts(),
                board.player1Score(),
                board.player2Score(),
                winner
        );
    }
}
