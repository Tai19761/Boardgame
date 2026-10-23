package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.services.InvalidMoveException;
import com.project.boardgame.application.services.MatchStatus;
import com.project.boardgame.application.services.RoomJoinException;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.application.services.RoomNotFoundException;
import com.project.boardgame.application.services.UnauthorizedSpectatorException;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import com.project.boardgame.infrastructure.web.websocket.dto.CreateRoomRequest;
import com.project.boardgame.infrastructure.web.websocket.dto.CreateRoomResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.ErrorResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.GameStateResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.JoinRoomRequest;
import com.project.boardgame.infrastructure.web.websocket.dto.MoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MatchControllerTest {

    private RoomManagerService roomManager;
    private TestMessagingTemplate messagingTemplate;
    private MatchController controller;

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
        controller = new MatchController(roomManager, messagingTemplate);
    }

    private SimpMessageHeaderAccessor createHeaderAccessor(String playerId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setUser(new StompPrincipal(playerId));
        return headerAccessor;
    }

    @Test
    @DisplayName("Tạo phòng: trả về CreateRoomResponse chứa roomId và lưu vào RoomManagerService")
    void testCreateRoom_Success() {
        SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor("host-user");
        CreateRoomRequest request = new CreateRoomRequest(GameType.CARO);

        CreateRoomResponse response = controller.createRoom(request, headerAccessor);

        assertNotNull(response);
        assertNotNull(response.roomId());
        assertTrue(roomManager.getRoom(response.roomId()).isPresent());
        assertEquals("host-user", roomManager.getRoom(response.roomId()).get().getHostId());
    }

    @Test
    @DisplayName("Join phòng: broadcast GameStateResponse đến /topic/match/{roomId}")
    void testJoinRoom_BroadcastsInitialState() {
        SimpMessageHeaderAccessor hostHeader = createHeaderAccessor("host-user");
        CreateRoomResponse createResponse = controller.createRoom(new CreateRoomRequest(GameType.CARO), hostHeader);
        String roomId = createResponse.roomId();

        SimpMessageHeaderAccessor guestHeader = createHeaderAccessor("guest-user");
        JoinRoomRequest joinRequest = new JoinRoomRequest(roomId);

        controller.joinRoom(joinRequest, guestHeader);

        assertEquals("/topic/match/" + roomId, messagingTemplate.lastDestination);
        assertNotNull(messagingTemplate.lastPayload);
        assertTrue(messagingTemplate.lastPayload instanceof GameStateResponse);

        GameStateResponse gameState = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(roomId, gameState.roomId());
        assertEquals(MatchStatus.ONGOING, gameState.matchStatus());
        assertEquals(Player.PLAYER_1, gameState.currentPlayer());
        assertEquals(GameType.CARO, gameState.gameType(), "Response phải chứa gameType CARO");
    }

    @Test
    @DisplayName("Join phòng Cờ Gánh: broadcast GameStateResponse chứa gameType GANH")
    void testJoinRoom_GanhGame_BroadcastsInitialStateWithGanhType() {
        SimpMessageHeaderAccessor hostHeader = createHeaderAccessor("host-ganh");
        CreateRoomResponse createResponse = controller.createRoom(new CreateRoomRequest(GameType.GANH), hostHeader);
        String roomId = createResponse.roomId();

        SimpMessageHeaderAccessor guestHeader = createHeaderAccessor("guest-ganh");
        JoinRoomRequest joinRequest = new JoinRoomRequest(roomId);

        controller.joinRoom(joinRequest, guestHeader);

        assertEquals("/topic/match/" + roomId, messagingTemplate.lastDestination);
        assertTrue(messagingTemplate.lastPayload instanceof GameStateResponse);

        GameStateResponse gameState = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(roomId, gameState.roomId());
        assertEquals(MatchStatus.ONGOING, gameState.matchStatus());
        assertEquals(GameType.GANH, gameState.gameType(), "Response phải chứa gameType GANH");
    }

    @Test
    @DisplayName("Đi nước hợp lệ: cập nhật trạng thái bàn cờ và broadcast ra topic")
    void testHandleMove_ValidMove_BroadcastsNewState() {
        String roomId = roomManager.createRoom("host-user", GameType.CARO);
        roomManager.joinRoom(roomId, "guest-user");

        Principal hostPrincipal = new StompPrincipal("host-user");
        MoveRequest moveRequest = new MoveRequest(null, new Position(7, 7));

        controller.handleMove(roomId, moveRequest, hostPrincipal);

        assertEquals("/topic/match/" + roomId, messagingTemplate.lastDestination);
        GameStateResponse state = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(Player.PLAYER_2, state.currentPlayer(), "Lượt tiếp theo phải là PLAYER_2");
        assertNotNull(state.pieces().get(new Position(7, 7)));
    }

    @Test
    @DisplayName("Spectator (người ngoài) gửi nước đi: ném UnauthorizedSpectatorException")
    void testHandleMove_Spectator_ThrowsUnauthorizedSpectatorException() {
        String roomId = roomManager.createRoom("host-user", GameType.CARO);
        roomManager.joinRoom(roomId, "guest-user");

        Principal spectatorPrincipal = new StompPrincipal("spectator-user");
        MoveRequest moveRequest = new MoveRequest(null, new Position(7, 7));

        UnauthorizedSpectatorException ex = assertThrows(
                UnauthorizedSpectatorException.class,
                () -> controller.handleMove(roomId, moveRequest, spectatorPrincipal)
        );

        assertTrue(ex.getMessage().contains("Người ngoài không có quyền"));
    }

    @Test
    @DisplayName("Nước đi trong phòng không tồn tại: ném RoomNotFoundException")
    void testHandleMove_RoomNotFound_ThrowsRoomNotFoundException() {
        Principal principal = new StompPrincipal("any-user");
        MoveRequest moveRequest = new MoveRequest(null, new Position(7, 7));

        assertThrows(
                RoomNotFoundException.class,
                () -> controller.handleMove("non-existent-room", moveRequest, principal)
        );
    }

    @Test
    @DisplayName("Nước đi sai luật/lượt: ném InvalidMoveException")
    void testHandleMove_InvalidTurn_ThrowsInvalidMoveException() {
        String roomId = roomManager.createRoom("host-user", GameType.CARO);
        roomManager.joinRoom(roomId, "guest-user");

        // guest đi trước khi tới lượt của mình
        Principal guestPrincipal = new StompPrincipal("guest-user");
        MoveRequest moveRequest = new MoveRequest(null, new Position(7, 7));

        assertThrows(
                InvalidMoveException.class,
                () -> controller.handleMove(roomId, moveRequest, guestPrincipal)
        );
    }

    @Test
    @DisplayName("Khi ván cờ kết thúc bình thường: dọn dẹp playerToRoomMap cho cả 2 người")
    void testHandleMove_MatchFinished_CleansPlayerMappings() {
        String roomId = roomManager.createRoom("host-user", GameType.CARO);
        roomManager.joinRoom(roomId, "guest-user");

        Principal hostPrincipal = new StompPrincipal("host-user");
        Principal guestPrincipal = new StompPrincipal("guest-user");

        // Đi 4 cặp nước đầu tiên:
        // P1: (0,0), P2: (0,1)
        controller.handleMove(roomId, new MoveRequest(null, new Position(0, 0)), hostPrincipal);
        controller.handleMove(roomId, new MoveRequest(null, new Position(0, 1)), guestPrincipal);
        // P1: (1,0), P2: (1,1)
        controller.handleMove(roomId, new MoveRequest(null, new Position(1, 0)), hostPrincipal);
        controller.handleMove(roomId, new MoveRequest(null, new Position(1, 1)), guestPrincipal);
        // P1: (2,0), P2: (2,1)
        controller.handleMove(roomId, new MoveRequest(null, new Position(2, 0)), hostPrincipal);
        controller.handleMove(roomId, new MoveRequest(null, new Position(2, 1)), guestPrincipal);
        // P1: (3,0), P2: (3,1)
        controller.handleMove(roomId, new MoveRequest(null, new Position(3, 0)), hostPrincipal);
        controller.handleMove(roomId, new MoveRequest(null, new Position(3, 1)), guestPrincipal);

        // Ván vẫn ONGOING, map vẫn còn
        assertEquals(Optional.of(roomId), roomManager.getRoomIdByPlayer("host-user"));

        // Nước thắng thứ 5 của P1: (4,0)
        controller.handleMove(roomId, new MoveRequest(null, new Position(4, 0)), hostPrincipal);

        GameStateResponse finalState = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(MatchStatus.WIN_PLAYER_1, finalState.matchStatus());
        assertEquals(Player.PLAYER_1, finalState.winner());

        // Kiểm tra playerToRoomMap đã được dọn dẹp tự động
        assertTrue(roomManager.getRoomIdByPlayer("host-user").isEmpty());
        assertTrue(roomManager.getRoomIdByPlayer("guest-user").isEmpty());

        // Kiểm tra phòng đã được xóa hoàn toàn khỏi RoomManagerService để chống Zombie Room
        assertTrue(roomManager.getRoom(roomId).isEmpty(), "Phòng phải bị xóa khỏi RoomManager sau khi kết thúc để chống Zombie Room");
    }

    @Test
    @DisplayName("Các @MessageExceptionHandler trả về đúng ErrorResponse")
    void testMessageExceptionHandlers() {
        ErrorResponse r0 = controller.handleIllegalStateException(new IllegalStateException("Đang trong phòng khác"));
        assertEquals("Đang trong phòng khác", r0.message());
        assertEquals("ILLEGAL_STATE", r0.errorType());

        ErrorResponse r1 = controller.handleInvalidMoveException(new InvalidMoveException("Nước đi sai"));
        assertEquals("Nước đi sai", r1.message());
        assertEquals("INVALID_MOVE", r1.errorType());

        ErrorResponse r2 = controller.handleUnauthorizedSpectatorException(new UnauthorizedSpectatorException("Người ngoài"));
        assertEquals("Người ngoài", r2.message());
        assertEquals("UNAUTHORIZED_SPECTATOR", r2.errorType());

        ErrorResponse r3 = controller.handleRoomNotFoundException(new RoomNotFoundException("Không tìm thấy phòng"));
        assertEquals("Không tìm thấy phòng", r3.message());
        assertEquals("ROOM_NOT_FOUND", r3.errorType());

        ErrorResponse r4 = controller.handleRoomJoinException(new RoomJoinException("Phòng đầy"));
        assertEquals("Phòng đầy", r4.message());
        assertEquals("ROOM_JOIN_ERROR", r4.errorType());

        ErrorResponse r5 = controller.handleGenericException(new RuntimeException("Lỗi hệ thống"));
        assertEquals("Lỗi hệ thống", r5.message());
        assertEquals("SYSTEM_ERROR", r5.errorType());
    }

    @Test
    @DisplayName("Tạo phòng O_AN_QUAN và vào phòng: trả về đúng GameType và pieceCounts")
    void testCreateAndJoinRoom_OAnQuan_ReturnsCorrectGameTypeAndPieceCounts() {
        SimpMessageHeaderAccessor hostHeader = createHeaderAccessor("host-oanquan");
        CreateRoomResponse createResponse = controller.createRoom(new CreateRoomRequest(GameType.O_AN_QUAN), hostHeader);
        String roomId = createResponse.roomId();

        SimpMessageHeaderAccessor guestHeader = createHeaderAccessor("guest-oanquan");
        controller.joinRoom(new JoinRoomRequest(roomId), guestHeader);

        assertEquals("/topic/match/" + roomId, messagingTemplate.lastDestination);
        assertTrue(messagingTemplate.lastPayload instanceof GameStateResponse);

        GameStateResponse gameState = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(roomId, gameState.roomId());
        assertEquals(MatchStatus.ONGOING, gameState.matchStatus());
        assertEquals(GameType.O_AN_QUAN, gameState.gameType());
        assertNotNull(gameState.pieceCounts());
        assertEquals(12, gameState.pieceCounts().size(), "Ô Ăn Quan có 12 ô (2 quan + 10 dân)");
    }

    @Test
    @DisplayName("Ô Ăn Quan: gửi nước đi bằng direction 'RIGHT' được dịch chính xác thành MoveStep và thực thi")
    void testHandleMove_OAnQuan_WithDirection_TranslatesToMoveStepAndExecutes() {
        SimpMessageHeaderAccessor hostHeader = createHeaderAccessor("host-oanquan");
        CreateRoomResponse createResponse = controller.createRoom(new CreateRoomRequest(GameType.O_AN_QUAN), hostHeader);
        String roomId = createResponse.roomId();

        SimpMessageHeaderAccessor guestHeader = createHeaderAccessor("guest-oanquan");
        controller.joinRoom(new JoinRoomRequest(roomId), guestHeader);

        Principal hostPrincipal = new StompPrincipal("host-oanquan");
        // P1 đi từ ô (1,1) sang PHẢI (direction = "RIGHT")
        MoveRequest moveRequest = new MoveRequest(new Position(1, 1), null, "RIGHT");

        controller.handleMove(roomId, moveRequest, hostPrincipal);

        assertEquals("/topic/match/" + roomId, messagingTemplate.lastDestination);
        GameStateResponse state = (GameStateResponse) messagingTemplate.lastPayload;
        assertEquals(Player.PLAYER_2, state.currentPlayer(), "Lượt tiếp theo phải đổi sang PLAYER_2");
        assertEquals(0, state.pieceCounts().get(new Position(1, 1)), "Ô xuất phát (1,1) phải hết sỏi");
        assertEquals(11, state.pieceCounts().get(new Position(6, 0)), "Quan Phải (6,0) nhận 1 viên sỏi (10 + 1 = 11)");
    }
}
