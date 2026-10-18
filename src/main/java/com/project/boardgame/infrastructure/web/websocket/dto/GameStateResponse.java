package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.GameType;
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
        Player winner,
        GameType gameType
) {

    public static GameStateResponse from(String roomId, Room room) {
        MatchEngineService engine = room.getMatchEngine();
        BoardState board = engine.getCurrentBoard();
        MatchStatus status = engine.getMatchStatus();
        GameType gameType = room.getGameType();

        Player winner = null;
        if (status == MatchStatus.WIN_PLAYER_1) {
            winner = Player.PLAYER_1;
        } else if (status == MatchStatus.WIN_PLAYER_2) {
            winner = Player.PLAYER_2;
        }

        if (board == null) {
            return new GameStateResponse(
                    roomId,
                    status != null ? status : MatchStatus.ONGOING,
                    null,
                    Map.of(),
                    Map.of(),
                    0,
                    0,
                    null,
                    gameType
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
                winner,
                gameType
        );
    }
}
