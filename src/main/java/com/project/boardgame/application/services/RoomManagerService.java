package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.PlayerDisconnectInfo;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.ports.GameRuleEngineFactory;
import com.project.boardgame.domain.ai.AiDifficulty;
import com.project.boardgame.domain.rules.GameRuleEngine;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RoomManagerService {

    private final GameRuleEngineFactory ruleEngineFactory;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerToRoomMap = new ConcurrentHashMap<>();

    /** Whitelist kích thước bàn Caro được phép — chặn client gửi giá trị lớn gây cạn kiệt bộ nhớ/CPU. */
    public static final Set<Integer> ALLOWED_CARO_BOARD_SIZES = Set.of(15, 20, 25);
    public static final int DEFAULT_CARO_BOARD_SIZE = 15;

    public RoomManagerService(GameRuleEngineFactory ruleEngineFactory) {
        this.ruleEngineFactory = ruleEngineFactory;
    }

    /**
     * Chuẩn hóa và kiểm tra {@code boardSize} theo loại game.
     * CARO: null → 15; ngoài whitelist → {@link IllegalArgumentException}. Game khác: luôn null.
     */
    static Integer resolveBoardSize(GameType gameType, Integer requested) {
        if (gameType != GameType.CARO) {
            return null;
        }
        if (requested == null) {
            return DEFAULT_CARO_BOARD_SIZE;
        }
        if (!ALLOWED_CARO_BOARD_SIZES.contains(requested)) {
            throw new IllegalArgumentException("Kích thước bàn cờ không hợp lệ: " + requested
                    + ". Chỉ hỗ trợ 15, 20 hoặc 25");
        }
        return requested;
    }

    public String createRoom(String playerId, GameType gameType) {
        return createRoom(playerId, gameType, null);
    }

    public String createRoom(String playerId, GameType gameType, Integer boardSize) {
        // Validate trước khi chiếm chỗ trong playerToRoomMap
        Integer resolvedSize = resolveBoardSize(gameType, boardSize);
        if (playerToRoomMap.putIfAbsent(playerId, "PENDING") != null) {
            throw new IllegalStateException("Người chơi đang ở trong phòng khác hoặc đang tạo phòng");
        }

        try {
            GameRuleEngine engine = ruleEngineFactory.create(gameType, resolvedSize);
            MatchEngineService matchEngine = new MatchEngineService();
            Room room = new Room(playerId, gameType, engine, matchEngine, resolvedSize);
            rooms.put(room.getRoomId(), room);
            playerToRoomMap.put(playerId, room.getRoomId());
            return room.getRoomId();
        } catch (Exception e) {
            playerToRoomMap.remove(playerId, "PENDING");
            throw e;
        }
    }

    public Room createBotRoom(String playerId, GameType gameType) {
        return createBotRoom(playerId, gameType, null, AiDifficulty.NORMAL);
    }

    public Room createBotRoom(String playerId, GameType gameType, Integer boardSize) {
        return createBotRoom(playerId, gameType, boardSize, AiDifficulty.NORMAL);
    }

    /**
     * Tạo phòng đấu với máy (chỉ hỗ trợ CARO). Phòng vào trạng thái PLAYING và ván ONGOING ngay,
     * guestId = {@link Room#BOT_ID}. Bot không được ghi vào playerToRoomMap.
     */
    public Room createBotRoom(String playerId, GameType gameType, Integer boardSize, AiDifficulty difficulty) {
        if (gameType != GameType.CARO) {
            throw new IllegalArgumentException("Chế độ đấu với máy hiện chỉ hỗ trợ Cờ Caro");
        }
        Integer resolvedSize = resolveBoardSize(gameType, boardSize);
        AiDifficulty resolvedDifficulty = difficulty != null ? difficulty : AiDifficulty.NORMAL;
        if (playerToRoomMap.putIfAbsent(playerId, "PENDING") != null) {
            throw new IllegalStateException("Người chơi đang ở trong phòng khác hoặc đang tạo phòng");
        }

        try {
            GameRuleEngine engine = ruleEngineFactory.create(gameType, resolvedSize);
            Room room = Room.createBotMatch(playerId, gameType, engine, new MatchEngineService(), resolvedSize, resolvedDifficulty);
            rooms.put(room.getRoomId(), room);
            playerToRoomMap.put(playerId, room.getRoomId());
            return room;
        } catch (Exception e) {
            playerToRoomMap.remove(playerId, "PENDING");
            throw e;
        }
    }

    public Room joinRoom(String roomId, String playerId) {
        if (playerToRoomMap.putIfAbsent(playerId, "PENDING") != null) {
            throw new IllegalStateException("Người chơi đang ở trong phòng khác hoặc đang vào phòng");
        }

        Room joinedRoom;
        try {
            joinedRoom = rooms.computeIfPresent(roomId, (id, room) -> {
                if (room.getRoomStatus() != RoomStatus.WAITING || room.getGuestId() != null) {
                    throw new RoomJoinException("Phòng đã đủ người hoặc ván đã bắt đầu");
                }
                room.joinGuest(playerId);
                return room;
            });

            if (joinedRoom == null) {
                throw new RoomJoinException("Phòng không còn tồn tại");
            }
        } catch (Exception e) {
            playerToRoomMap.remove(playerId, "PENDING");
            throw e;
        }

        playerToRoomMap.put(playerId, roomId);
        return joinedRoom;
    }

    public Optional<PlayerDisconnectInfo> handlePlayerDisconnect(String playerId) {
        String roomId = playerToRoomMap.remove(playerId);
        if (roomId == null || "PENDING".equals(roomId)) {
            return Optional.empty();
        }

        AtomicReference<PlayerDisconnectInfo> disconnectInfoRef = new AtomicReference<>();

        rooms.computeIfPresent(roomId, (id, room) -> {
            RoomStatus status = room.getRoomStatus();
            if (status == RoomStatus.WAITING) {
                disconnectInfoRef.set(new PlayerDisconnectInfo(
                        roomId,
                        RoomStatus.WAITING,
                        playerId,
                        null
                ));
                return null;
            } else if (status == RoomStatus.PLAYING) {
                String opponentId = playerId.equals(room.getHostId())
                        ? room.getGuestId()
                        : room.getHostId();
                disconnectInfoRef.set(new PlayerDisconnectInfo(
                        roomId,
                        RoomStatus.PLAYING,
                        playerId,
                        opponentId
                ));
                return null;
            }
            return null;
        });

        PlayerDisconnectInfo info = disconnectInfoRef.get();
        if (info != null && info.opponentId() != null) {
            playerToRoomMap.remove(info.opponentId());
        }

        return Optional.ofNullable(info);
    }

    public void removePlayerMappings(String hostId, String guestId) {
        if (hostId != null) {
            playerToRoomMap.remove(hostId);
        }
        if (guestId != null) {
            playerToRoomMap.remove(guestId);
        }
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

    public Optional<String> getRoomIdByPlayer(String playerId) {
        return Optional.ofNullable(playerToRoomMap.get(playerId));
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
}
