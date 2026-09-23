package com.project.boardgame.domain.models;

import java.util.Map;

public record BoardState(
    Map<Position, Piece> pieces,
    Map<Position, Integer> pieceCounts,
    int player1Score,
    int player2Score,
    Player currentPlayer
) {
    public BoardState {
        pieces = Map.copyOf(pieces);
        pieceCounts = Map.copyOf(pieceCounts);
    }

    public BoardState(Map<Position, Piece> pieces, Player currentPlayer) {
        this(pieces, Map.of(), 0, 0, currentPlayer);
    }
}
