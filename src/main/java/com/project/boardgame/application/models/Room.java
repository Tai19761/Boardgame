package com.project.boardgame.application.models;

import com.project.boardgame.application.services.MatchEngineService;
import com.project.boardgame.domain.rules.GameRuleEngine;

import java.util.UUID;

public class Room {

    private final String roomId;
    private final String hostId;
    private String guestId;
    private final MatchEngineService matchEngine;
    private RoomStatus roomStatus;
    private final GameType gameType;
    private final GameRuleEngine ruleEngine;

    public Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine) {
        this.roomId = UUID.randomUUID().toString();
        this.hostId = hostId;
        this.guestId = null;
        this.gameType = gameType;
        this.ruleEngine = ruleEngine;
        this.matchEngine = matchEngine;
        this.roomStatus = RoomStatus.WAITING;
    }

    public void joinGuest(String guestId) {
        this.guestId = guestId;
        this.roomStatus = RoomStatus.PLAYING;
        this.matchEngine.startMatch(this.ruleEngine);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getGuestId() {
        return guestId;
    }

    public MatchEngineService getMatchEngine() {
        return matchEngine;
    }

    public RoomStatus getRoomStatus() {
        return roomStatus;
    }

    public GameType getGameType() {
        return gameType;
    }

    public GameRuleEngine getRuleEngine() {
        return ruleEngine;
    }
}
