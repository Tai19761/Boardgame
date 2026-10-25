package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.ports.MatchStateBroadcaster;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AiMoveServiceTest {

    /** Broadcaster giả: ghi lại roomId + trạng thái match tại thời điểm broadcast. */
    static class RecordingBroadcaster implements MatchStateBroadcaster {
        final List<String> roomIds = new ArrayList<>();
        final List<MatchStatus> statuses = new ArrayList<>();
        final List<Player> currentPlayers = new ArrayList<>();

        @Override
        public void broadcastState(String roomId, Room room) {
            roomIds.add(roomId);
            statuses.add(room.getMatchEngine().getMatchStatus());
            currentPlayers.add(room.getMatchEngine().getCurrentBoard().currentPlayer());
        }
    }

    private RoomManagerService roomManager;
    private RecordingBroadcaster broadcaster;
    private AiMoveService aiMoveService;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
        broadcaster = new RecordingBroadcaster();
        // Runnable::run → task AI chạy đồng bộ ngay trên thread test, kết quả kiểm tra được tức thì
        Executor synchronousExecutor = Runnable::run;
        aiMoveService = new AiMoveService(roomManager, broadcaster, synchronousExecutor);
    }

    private static MoveStep at(int x, int y) {
        return new MoveStep(null, new Position(x, y));
    }

    @Test
    @DisplayName("Đến lượt Bot: Bot thực hiện 1 nước hợp lệ, trả lượt cho PLAYER_1 và broadcast đúng 1 lần")
    void scheduleBotMove_BotTurn_ExecutesMoveAndBroadcasts() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);

        aiMoveService.scheduleBotMove(room.getRoomId());

        assertEquals(2, room.getMatchEngine().getCurrentBoard().pieces().size(), "Bot phải đặt thêm 1 quân");
        assertEquals(Player.PLAYER_1, room.getMatchEngine().getCurrentBoard().currentPlayer());
        assertEquals(List.of(room.getRoomId()), broadcaster.roomIds);
        assertEquals(MatchStatus.ONGOING, broadcaster.statuses.get(0));
        assertEquals(Player.PLAYER_1, broadcaster.currentPlayers.get(0));
    }

    @Test
    @DisplayName("Phòng không tồn tại: bỏ qua, không broadcast, không ném lỗi")
    void scheduleBotMove_RoomMissing_IsIgnored() {
        assertDoesNotThrow(() -> aiMoveService.scheduleBotMove("ghost-room"));
        assertTrue(broadcaster.roomIds.isEmpty());
    }

    @Test
    @DisplayName("Chưa đến lượt Bot (lượt PLAYER_1): bỏ qua, bàn cờ không đổi")
    void scheduleBotMove_NotBotTurn_IsIgnored() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);

        aiMoveService.scheduleBotMove(room.getRoomId());

        assertTrue(room.getMatchEngine().getCurrentBoard().pieces().isEmpty());
        assertTrue(broadcaster.roomIds.isEmpty());
    }

    @Test
    @DisplayName("Phòng người-người (không phải botMatch): bỏ qua")
    void scheduleBotMove_NonBotRoom_IsIgnored() {
        String roomId = roomManager.createRoom("host", GameType.CARO);
        Room room = roomManager.joinRoom(roomId, "guest");
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);

        aiMoveService.scheduleBotMove(roomId);

        assertEquals(1, room.getMatchEngine().getCurrentBoard().pieces().size());
        assertTrue(broadcaster.roomIds.isEmpty());
    }

    @Test
    @DisplayName("Phòng đã kết thúc (FINISHED): bỏ qua")
    void scheduleBotMove_FinishedRoom_IsIgnored() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);
        room.finishRoom();

        aiMoveService.scheduleBotMove(room.getRoomId());

        assertEquals(1, room.getMatchEngine().getCurrentBoard().pieces().size());
        assertTrue(broadcaster.roomIds.isEmpty());
    }

    @Test
    @DisplayName("Bot đi nước thắng: ván kết thúc WIN_PLAYER_2, phòng bị xóa và mapping người chơi được dọn")
    void scheduleBotMove_WinningMove_FinishesAndCleansUpRoom() {
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        String roomId = room.getRoomId();
        var engine = room.getMatchEngine();
        engine.executeMove(at(0, 0), Player.PLAYER_1);
        engine.executeMove(at(5, 5), Player.PLAYER_2);
        engine.executeMove(at(0, 2), Player.PLAYER_1);
        engine.executeMove(at(6, 5), Player.PLAYER_2);
        engine.executeMove(at(0, 4), Player.PLAYER_1);
        engine.executeMove(at(7, 5), Player.PLAYER_2);
        engine.executeMove(at(0, 6), Player.PLAYER_1);
        engine.executeMove(at(8, 5), Player.PLAYER_2);
        engine.executeMove(at(0, 8), Player.PLAYER_1);

        aiMoveService.scheduleBotMove(roomId);

        assertEquals(MatchStatus.WIN_PLAYER_2, engine.getMatchStatus());
        assertEquals(List.of(MatchStatus.WIN_PLAYER_2), broadcaster.statuses);
        assertEquals(RoomStatus.FINISHED, room.getRoomStatus());
        assertTrue(roomManager.getRoom(roomId).isEmpty(), "Phòng phải bị xóa (chống Zombie Room)");
        assertTrue(roomManager.getRoomIdByPlayer("human").isEmpty(), "Mapping người chơi phải được dọn");
    }

    @Test
    @DisplayName("Lỗi trong task AI (broadcast ném exception) được bắt lại, không lan ra thread gọi")
    void scheduleBotMove_ExceptionInsideTask_IsCaught() {
        MatchStateBroadcaster failing = (roomId, room) -> {
            throw new IllegalStateException("broker down");
        };
        AiMoveService service = new AiMoveService(roomManager, failing, Runnable::run);
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);

        assertDoesNotThrow(() -> service.scheduleBotMove(room.getRoomId()));
    }

    @Test
    @DisplayName("scheduleBotMove chỉ đẩy task vào executor, không tự chạy trên thread gọi")
    void scheduleBotMove_SubmitsTaskToExecutor() {
        List<Runnable> queued = new ArrayList<>();
        AiMoveService service = new AiMoveService(roomManager, broadcaster, queued::add);
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);

        service.scheduleBotMove(room.getRoomId());

        assertEquals(1, queued.size(), "Phải có đúng 1 task được đẩy vào pool");
        assertEquals(1, room.getMatchEngine().getCurrentBoard().pieces().size(), "Chưa chạy task thì bàn cờ chưa đổi");

        queued.get(0).run();

        assertEquals(2, room.getMatchEngine().getCurrentBoard().pieces().size());
    }

    @Test
    @DisplayName("Chống race: nếu bàn cờ đã thay đổi trong lúc Bot đang tính (lượt không còn của Bot) thì bỏ kết quả")
    void scheduleBotMove_StateChangedWhileThinking_DiscardsResult() {
        List<Runnable> queued = new ArrayList<>();
        AiMoveService service = new AiMoveService(roomManager, broadcaster, queued::add);
        Room room = roomManager.createBotRoom("human", GameType.CARO);
        room.getMatchEngine().executeMove(at(7, 7), Player.PLAYER_1);

        service.scheduleBotMove(room.getRoomId());
        // Phòng bị xóa (vd: người chơi ngắt kết nối) trước khi task AI chạy
        roomManager.removeRoom(room.getRoomId());
        queued.get(0).run();

        assertEquals(1, room.getMatchEngine().getCurrentBoard().pieces().size());
        assertTrue(broadcaster.roomIds.isEmpty());
    }
}
