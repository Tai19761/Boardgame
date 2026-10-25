package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.services.AiMoveService;
import com.project.boardgame.application.services.MatchStatus;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import com.project.boardgame.infrastructure.web.websocket.dto.CreateRoomRequest;
import com.project.boardgame.infrastructure.web.websocket.dto.GameStateResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.MoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchControllerBotMatchTest {

    static class RecordingMessagingTemplate extends SimpMessagingTemplate {
        final List<String> destinations = new ArrayList<>();
        final List<Object> payloads = new ArrayList<>();

        RecordingMessagingTemplate() {
            super(new ExecutorSubscribableChannel());
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            destinations.add(destination);
            payloads.add(payload);
        }
    }

    private RoomManagerService roomManager;
    private RecordingMessagingTemplate messagingTemplate;
    private List<Runnable> queuedAiTasks;
    private MatchController controller;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
        messagingTemplate = new RecordingMessagingTemplate();
        queuedAiTasks = new ArrayList<>();
        AiMoveService aiMoveService = new AiMoveService(
                roomManager,
                new StompMatchStateBroadcaster(messagingTemplate),
                queuedAiTasks::add
        );
        controller = new MatchController(roomManager, messagingTemplate, aiMoveService);
    }

    private SimpMessageHeaderAccessor header(String playerId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setUser(new StompPrincipal(playerId));
        return accessor;
    }

    @Test
    @DisplayName("Tạo phòng đấu máy: trả về GameStateResponse ONGOING, lượt PLAYER_1, bàn trống")
    void createBotRoom_ReturnsInitialOngoingState() {
        GameStateResponse state = controller.createBotRoom(new CreateRoomRequest(GameType.CARO), header("human"));

        assertNotNull(state.roomId());
        assertEquals(MatchStatus.ONGOING, state.matchStatus());
        assertEquals(Player.PLAYER_1, state.currentPlayer());
        assertTrue(state.pieces().isEmpty());
        assertEquals(GameType.CARO, state.gameType());
        assertTrue(roomManager.getRoom(state.roomId()).orElseThrow().isBotMatch());
        assertTrue(queuedAiTasks.isEmpty(), "Người chơi đi trước nên chưa được lên lịch Bot");
    }

    @Test
    @DisplayName("Người chơi đi trong phòng đấu máy: broadcast nước của người trước, sau đó lên lịch Bot; Bot đi xong broadcast tiếp")
    void humanMoveInBotRoom_BroadcastsThenSchedulesBot() {
        String roomId = controller.createBotRoom(new CreateRoomRequest(GameType.CARO), header("human")).roomId();
        Principal human = new StompPrincipal("human");

        controller.handleMove(roomId, new MoveRequest(null, new Position(7, 7)), human);

        assertEquals(1, messagingTemplate.payloads.size(), "Nước của người chơi phải được broadcast trước");
        GameStateResponse afterHuman = (GameStateResponse) messagingTemplate.payloads.get(0);
        assertEquals(Player.PLAYER_2, afterHuman.currentPlayer());
        assertEquals(1, queuedAiTasks.size(), "Phải lên lịch đúng 1 nước Bot");

        queuedAiTasks.get(0).run();

        assertEquals(2, messagingTemplate.payloads.size());
        assertEquals("/topic/match/" + roomId, messagingTemplate.destinations.get(1));
        GameStateResponse afterBot = (GameStateResponse) messagingTemplate.payloads.get(1);
        assertEquals(Player.PLAYER_1, afterBot.currentPlayer(), "Bot đi xong phải trả lượt cho người chơi");
        assertEquals(2, afterBot.pieces().size());
        assertEquals(MatchStatus.ONGOING, afterBot.matchStatus());
    }

    @Test
    @DisplayName("Phòng người-người: không bao giờ lên lịch Bot")
    void humanMoveInNormalRoom_DoesNotScheduleBot() {
        String roomId = roomManager.createRoom("host", GameType.CARO);
        roomManager.joinRoom(roomId, "guest");

        controller.handleMove(roomId, new MoveRequest(null, new Position(7, 7)), new StompPrincipal("host"));

        assertTrue(queuedAiTasks.isEmpty());
    }

    @Test
    @DisplayName("Người chơi thắng trong phòng đấu máy: ván kết thúc, không lên lịch Bot")
    void humanWinsInBotRoom_DoesNotScheduleBot() {
        String roomId = controller.createBotRoom(new CreateRoomRequest(GameType.CARO), header("human")).roomId();
        var engine = roomManager.getRoom(roomId).orElseThrow().getMatchEngine();
        // Dựng thế cờ: người chơi có 4 quân (0..3, 0), Bot có 4 quân rải rác
        engine.executeMove(new MoveStep(null, new Position(0, 0)), Player.PLAYER_1);
        engine.executeMove(new MoveStep(null, new Position(10, 10)), Player.PLAYER_2);
        engine.executeMove(new MoveStep(null, new Position(1, 0)), Player.PLAYER_1);
        engine.executeMove(new MoveStep(null, new Position(12, 10)), Player.PLAYER_2);
        engine.executeMove(new MoveStep(null, new Position(2, 0)), Player.PLAYER_1);
        engine.executeMove(new MoveStep(null, new Position(10, 12)), Player.PLAYER_2);
        engine.executeMove(new MoveStep(null, new Position(3, 0)), Player.PLAYER_1);
        engine.executeMove(new MoveStep(null, new Position(12, 12)), Player.PLAYER_2);

        controller.handleMove(roomId, new MoveRequest(null, new Position(4, 0)), new StompPrincipal("human"));

        GameStateResponse last = (GameStateResponse) messagingTemplate.payloads.get(messagingTemplate.payloads.size() - 1);
        assertEquals(MatchStatus.WIN_PLAYER_1, last.matchStatus());
        assertTrue(queuedAiTasks.isEmpty());
        assertTrue(roomManager.getRoom(roomId).isEmpty());
    }
}
