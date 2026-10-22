package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import com.project.boardgame.infrastructure.web.websocket.dto.PlayerDisconnectedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketEventListenerTest {

    private RoomManagerService roomManager;
    private TestMessagingTemplate messagingTemplate;
    private WebSocketEventListener eventListener;

    static class TestMessagingTemplate extends SimpMessagingTemplate {
        String lastDestination;
        Object lastPayload;

        public TestMessagingTemplate() {
            super(new ExecutorSubscribableChannel());
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            this.lastDestination = destination;
            this.lastPayload = payload;
        }
    }

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
        messagingTemplate = new TestMessagingTemplate();
        eventListener = new WebSocketEventListener(roomManager, messagingTemplate);
    }

    private SessionDisconnectEvent createDisconnectEvent(String sessionId, Principal user) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setUser(user);
        Message<byte[]> message = new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL, user);
    }

    @Test
    @DisplayName("Player disconnect khi đang chơi (PLAYING): dọn dẹp phòng và broadcast báo đối thủ rớt mạng")
    void testHandleSessionDisconnect_PlayingRoom_NotifiesOpponent() {
        String hostId = "user-p1";
        String guestId = "user-p2";
        String roomId = roomManager.createRoom(hostId, GameType.CARO);
        roomManager.joinRoom(roomId, guestId);

        SessionDisconnectEvent event = createDisconnectEvent("session-1", new StompPrincipal(hostId));
        eventListener.handleSessionDisconnect(event);

        assertEquals("/topic/match/" + roomId + "/events", messagingTemplate.lastDestination);
        assertNotNull(messagingTemplate.lastPayload);
        assertTrue(messagingTemplate.lastPayload instanceof PlayerDisconnectedResponse);

        PlayerDisconnectedResponse payload = (PlayerDisconnectedResponse) messagingTemplate.lastPayload;
        assertEquals(roomId, payload.roomId());
        assertEquals(hostId, payload.disconnectedPlayerId());

        // Phòng đã bị xóa
        assertTrue(roomManager.getRoom(roomId).isEmpty());
    }

    @Test
    @DisplayName("Player disconnect khi đang chờ (WAITING): xóa phòng và không cần broadcast đối thủ")
    void testHandleSessionDisconnect_WaitingRoom_RemovesRoomQuietly() {
        String hostId = "user-waiting";
        String roomId = roomManager.createRoom(hostId, GameType.CARO);

        SessionDisconnectEvent event = createDisconnectEvent("session-2", new StompPrincipal(hostId));
        eventListener.handleSessionDisconnect(event);

        assertNull(messagingTemplate.lastDestination);
        assertTrue(roomManager.getRoom(roomId).isEmpty());
    }

    @Test
    @DisplayName("Event với user null: xử lý an toàn không văng exception")
    void testHandleSessionDisconnect_NullUser_Safe() {
        SessionDisconnectEvent event = createDisconnectEvent("session-3", null);
        assertDoesNotThrow(() -> eventListener.handleSessionDisconnect(event));
    }
}
