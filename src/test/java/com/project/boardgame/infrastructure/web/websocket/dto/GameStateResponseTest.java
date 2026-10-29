package com.project.boardgame.infrastructure.web.websocket.dto;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.services.MatchStatus;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.domain.models.Piece;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameStateResponse Unit Tests")
class GameStateResponseTest {

    private RoomManagerService roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
    }

    @Test
    @DisplayName("Constructor khởi tạo đúng các trường bao gồm gameType")
    void testConstructor_InitializesAllFieldsIncludingGameType() {
        GameStateResponse response = new GameStateResponse(
                "room-123",
                MatchStatus.ONGOING,
                Player.PLAYER_1,
                Map.of(new Position(0, 0), new Piece(Player.PLAYER_1)),
                Map.of(new Position(0, 0), 1),
                1,
                0,
                null,
                GameType.GANH,
                null
        );

        assertEquals("room-123", response.roomId());
        assertEquals(MatchStatus.ONGOING, response.matchStatus());
        assertEquals(Player.PLAYER_1, response.currentPlayer());
        assertEquals(GameType.GANH, response.gameType());
        assertEquals(1, response.player1Score());
        assertEquals(0, response.player2Score());
        assertNull(response.winner());
        assertNotNull(response.pieces());
        assertNull(response.boardSize());
    }

    @Test
    @DisplayName("from(roomId, room) khi phòng CARO đã bắt đầu: trả về response có đúng gameType CARO")
    void testFrom_WithCaroRoom_ReturnsCorrectGameType() {
        String roomId = roomManager.createRoom("host-caro", GameType.CARO);
        roomManager.joinRoom(roomId, "guest-caro");
        Room room = roomManager.getRoom(roomId).orElseThrow();

        GameStateResponse response = GameStateResponse.from(roomId, room);

        assertNotNull(response);
        assertEquals(roomId, response.roomId());
        assertEquals(GameType.CARO, response.gameType());
        assertEquals(MatchStatus.ONGOING, response.matchStatus());
        assertEquals(Player.PLAYER_1, response.currentPlayer());
        assertEquals(15, response.boardSize(), "CARO không chỉ định kích thước thì mặc định 15");
    }

    @Test
    @DisplayName("from(roomId, room) phòng CARO 25x25: response mang đúng boardSize = 25")
    void testFrom_WithCaroRoom25_ReturnsBoardSize25() {
        String roomId = roomManager.createRoom("host-caro-25", GameType.CARO, 25);
        Room room = roomManager.getRoom(roomId).orElseThrow();

        GameStateResponse response = GameStateResponse.from(roomId, room);

        assertEquals(25, response.boardSize());
    }

    @Test
    @DisplayName("from(roomId, room) khi phòng GANH đã bắt đầu: trả về response có gameType GANH và 16 quân")
    void testFrom_WithGanhRoom_ReturnsGanhGameType() {
        String roomId = roomManager.createRoom("host-ganh", GameType.GANH);
        roomManager.joinRoom(roomId, "guest-ganh");
        Room room = roomManager.getRoom(roomId).orElseThrow();

        GameStateResponse response = GameStateResponse.from(roomId, room);

        assertNotNull(response);
        assertEquals(roomId, response.roomId());
        assertEquals(GameType.GANH, response.gameType());
        assertEquals(MatchStatus.ONGOING, response.matchStatus());
        assertEquals(16, response.pieces().size(), "Cờ Gánh khởi tạo có 16 quân cờ");
        assertNull(response.boardSize(), "Game khác Caro không có boardSize");
    }

    @Test
    @DisplayName("from(roomId, room) khi phòng đang chờ (board == null): trả về response có đúng gameType")
    void testFrom_WaitingRoomWithoutBoard_ReturnsGameType() {
        String roomId = roomManager.createRoom("host-waiting", GameType.GANH);
        Room room = roomManager.getRoom(roomId).orElseThrow();

        GameStateResponse response = GameStateResponse.from(roomId, room);

        assertNotNull(response);
        assertEquals(roomId, response.roomId());
        assertEquals(GameType.GANH, response.gameType());
        assertTrue(response.pieces().isEmpty(), "Phòng đang chờ chưa có quân cờ");
    }

    @Test
    @DisplayName("from(roomId, room) cho phòng Bot CARO: trả về đúng difficulty tương ứng")
    void testFrom_BotRoom_ReturnsDifficulty() {
        Room botRoom = roomManager.createBotRoom("host-bot-diff", GameType.CARO, 15, com.project.boardgame.domain.ai.AiDifficulty.HARD);
        GameStateResponse response = GameStateResponse.from(botRoom.getRoomId(), botRoom);

        assertNotNull(response);
        assertEquals(com.project.boardgame.domain.ai.AiDifficulty.HARD, response.difficulty());
    }
}
