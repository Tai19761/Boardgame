package com.project.boardgame.application.models;

import com.project.boardgame.application.services.MatchEngineService;
import com.project.boardgame.domain.ai.AiDifficulty;
import com.project.boardgame.domain.rules.CaroRuleEngine;
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
    /** Kích thước bàn (chỉ CARO); null với các game khác. Giá trị đã được RoomManagerService chuẩn hóa. */
    private final Integer boardSize;
    /** Cấp độ khó của AI (chỉ áp dụng cho phòng Đấu với Máy, phòng PvP luôn null). */
    private final AiDifficulty difficulty;

    public Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine) {
        this(hostId, gameType, ruleEngine, matchEngine, defaultBoardSize(gameType));
    }

    public Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, Integer boardSize) {
        this(hostId, gameType, ruleEngine, matchEngine, false, boardSize, null);
    }

    public Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, Integer boardSize, AiDifficulty difficulty) {
        this(hostId, gameType, ruleEngine, matchEngine, false, boardSize, difficulty);
    }

    private Room(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, boolean botMatch, Integer boardSize, AiDifficulty difficulty) {
        this.roomId = UUID.randomUUID().toString();
        this.hostId = hostId;
        this.guestId = null;
        this.gameType = gameType;
        this.ruleEngine = ruleEngine;
        this.matchEngine = matchEngine;
        this.roomStatus = RoomStatus.WAITING;
        this.botMatch = botMatch;
        this.boardSize = boardSize;
        this.difficulty = difficulty;
    }

    private static Integer defaultBoardSize(GameType gameType) {
        return gameType == GameType.CARO ? CaroRuleEngine.DEFAULT_BOARD_SIZE : null;
    }

    /**
     * Tạo phòng đấu với máy: guest là {@link #BOT_ID}, ván bắt đầu ngay (PLAYING / ONGOING),
     * người chơi luôn cầm PLAYER_1 và đi trước. Mặc định cấp độ NORMAL.
     */
    public static Room createBotMatch(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine) {
        return createBotMatch(hostId, gameType, ruleEngine, matchEngine, defaultBoardSize(gameType), AiDifficulty.NORMAL);
    }

    public static Room createBotMatch(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, Integer boardSize) {
        return createBotMatch(hostId, gameType, ruleEngine, matchEngine, boardSize, AiDifficulty.NORMAL);
    }

    public static Room createBotMatch(String hostId, GameType gameType, GameRuleEngine ruleEngine, MatchEngineService matchEngine, Integer boardSize, AiDifficulty difficulty) {
        Room room = new Room(hostId, gameType, ruleEngine, matchEngine, true, boardSize, difficulty != null ? difficulty : AiDifficulty.NORMAL);
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

    public Integer getBoardSize() {
        return boardSize;
    }

    public AiDifficulty getDifficulty() {
        return difficulty;
    }
}
