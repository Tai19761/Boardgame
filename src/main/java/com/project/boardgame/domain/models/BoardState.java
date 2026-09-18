package com.project.boardgame.domain.models;

import java.util.Map;

public record BoardState(Map<Position, Piece> pieces, Player currentPlayer) {
}
