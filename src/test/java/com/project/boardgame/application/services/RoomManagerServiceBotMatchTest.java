package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerServiceBotMatchTest {

    private RoomManagerService roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
    }

    @Test
    @DisplayName("Tạo phòng đấu máy: botMatch = true, guestId = BOT, phòng PLAYING và ván ONGOING ngay lập tức")
    void createBotRoom_StartsMatchImmediately() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);

        assertTrue(room.isBotMatch());
        assertEquals("human", room.getHostId());
        assertEquals(Room.BOT_ID, room.getGuestId());
        assertEquals("BOT", Room.BOT_ID);
        assertEquals(RoomStatus.PLAYING, room.getRoomStatus());
        assertEquals(MatchStatus.ONGOING, room.getMatchEngine().getMatchStatus());
        assertNotNull(room.getMatchEngine().getCurrentBoard());
        assertEquals(Player.PLAYER_1, room.getMatchEngine().getCurrentBoard().currentPlayer(), "Người chơi luôn đi trước");
        assertEquals(Optional.of(room), roomManager.getRoom(room.getRoomId()));
        assertEquals(Optional.of(room.getRoomId()), roomManager.getRoomIdByPlayer("human"));
    }

    @Test
    @DisplayName("Phòng thường vẫn có botMatch = false")
    void createRoom_IsNotBotMatch() {
        String roomId = roomManager.createRoom("host", GameType.CARO);

        assertFalse(roomManager.getRoom(roomId).orElseThrow().isBotMatch());
    }

    @Test
    @DisplayName("Đấu máy chỉ hỗ trợ CARO: game khác ném IllegalArgumentException và giải phóng mapping")
    void createBotRoom_NonCaro_ThrowsAndReleasesMapping() {
        assertThrows(IllegalArgumentException.class, () -> roomManager.createBotRoom("human", GameType.GANH));
        assertTrue(roomManager.getRoomIdByPlayer("human").isEmpty());

        // Có thể tạo lại ngay sau đó
        assertNotNull(roomManager.createBotRoom("human", GameType.CARO));
    }

    @Test
    @DisplayName("Người chơi đang ở phòng khác không thể tạo phòng đấu máy")
    void createBotRoom_PlayerAlreadyInRoom_Throws() {
        roomManager.createRoom("human", GameType.CARO);

        assertThrows(IllegalStateException.class, () -> roomManager.createBotRoom("human", GameType.CARO));
    }

    @Test
    @DisplayName("Không ai có thể join vào phòng đấu máy")
    void joinBotRoom_IsRejected() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);

        assertThrows(RoomJoinException.class, () -> roomManager.joinRoom(room.getRoomId(), "intruder"));
        assertTrue(roomManager.getRoomIdByPlayer("intruder").isEmpty());
    }

    @Test
    @DisplayName("Người chơi ngắt kết nối khỏi phòng đấu máy: phòng bị xóa")
    void disconnectFromBotRoom_RemovesRoom() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);

        roomManager.handlePlayerDisconnect("human");

        assertTrue(roomManager.getRoom(room.getRoomId()).isEmpty());
        assertTrue(roomManager.getRoomIdByPlayer("human").isEmpty());
    }
}
