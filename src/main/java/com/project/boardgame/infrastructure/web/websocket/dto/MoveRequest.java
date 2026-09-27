package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Position;

public record MoveRequest(Position from, Position to) {

    public MoveStep toMoveStep() {
        return new MoveStep(from, to);
    }
}
