package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.OAnQuanRuleEngine;

public record MoveRequest(Position from, Position to, String direction) {

    public MoveRequest(Position from, Position to) {
        this(from, to, null);
    }

    public MoveStep toMoveStep() {
        if (to != null) {
            return new MoveStep(from, to);
        }
        if (from != null && direction != null) {
            Position calculatedTo = calculateOAnQuanTo(from, direction);
            return new MoveStep(from, calculatedTo);
        }
        return new MoveStep(from, null);
    }

    private Position calculateOAnQuanTo(Position from, String dir) {
        String d = dir.trim().toUpperCase();
        if ("LEFT".equals(d)) {
            return (from.x() == 1) ? new Position(0, 0) : new Position(from.x() - 1, from.y());
        } else if ("RIGHT".equals(d)) {
            return (from.x() == 5) ? new Position(6, 0) : new Position(from.x() + 1, from.y());
        } else if ("CW".equals(d) || "CLOCKWISE".equals(d) || "FORWARD".equals(d)) {
            int fromIdx = OAnQuanRuleEngine.positionToIndex(from);
            if (fromIdx >= 0) {
                return OAnQuanRuleEngine.indexToPosition((fromIdx + 1) % OAnQuanRuleEngine.BOARD_SIZE);
            }
        } else if ("CCW".equals(d) || "COUNTER_CLOCKWISE".equals(d) || "BACKWARD".equals(d)) {
            int fromIdx = OAnQuanRuleEngine.positionToIndex(from);
            if (fromIdx >= 0) {
                return OAnQuanRuleEngine.indexToPosition((fromIdx + OAnQuanRuleEngine.BOARD_SIZE - 1) % OAnQuanRuleEngine.BOARD_SIZE);
            }
        }
        return null;
    }
}
