package com.project.boardgame.application.models;

import com.project.boardgame.application.services.MatchEngineService;
import com.project.boardgame.domain.rules.GameRuleEngine;

import java.util.UUID;

public class Room {

    /** Định danh cố định của máy (Bot) trong phòng đấu với máy. */
    public static final String BOT_ID = "BOT";

    private final String roomId;
    private final String hostId;
    private String guestId;
    private final MatchEngineService matchEngine;
    private RoomStatus roomStatus;
    private final GameType gameType;
    private final GameRuleEngine ruleEngine;
    private final boolean botMatch;

    public Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine) {
        this(hostId, gameType, ruleEngine, matchEngine, false);
    }

    private Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, boolean botMatch) {
        this.roomId = UUID.randomUUID().toString();
        this.hostId = hostId;
        this.guestId = null;
        this.gameType = gameType;
        this.ruleEngine = ruleEngine;
        this.matchEngine = matchEngine;
        this.roomStatus = RoomStatus.WAITING;
        this.botMatch = botMatch;
    }

    /**
     * Tạo phòng đấu với máy: guest là {@link #BOT_ID}, ván bắt đầu ngay (PLAYING / ONGOING),
     * người chơi luôn cầm PLAYER_1 và đi trước.
     */
    public static Room createBotMatch(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine) {
        Room room = new Room(hostId, gameType, ruleEngine, matchEngine, true);
        room.joinGuest(BOT_ID);
        return room;
    }

    public void joinGuest(String guestId) {
        this.guestId = guestId;
        this.roomStatus = RoomStatus.PLAYING;
        this.matchEngine.startMatch(this.ruleEngine);
    }

    public void finishRoom() {
        this.roomStatus = RoomStatus.FINISHED;
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

    public boolean isBotMatch() {
        return botMatch;
    }
}
