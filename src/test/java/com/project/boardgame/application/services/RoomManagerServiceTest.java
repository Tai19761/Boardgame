package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.infrastructure.config.DefaultGameRuleEngineFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerServiceTest {

    private RoomManagerService roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManagerService(new DefaultGameRuleEngineFactory());
    }

    @Test
    @DisplayName("Tạo phòng thành công: roomStatus = WAITING, guestId = null, matchEngine chưa start")
    void testCreateRoom_Success() {
        String roomId = roomManager.createRoom("host-1", GameType.CARO);
        assertNotNull(roomId);

        Optional<Room> roomOpt = roomManager.getRoom(roomId);
        assertTrue(roomOpt.isPresent());

        Room room = roomOpt.get();
        assertEquals(roomId, room.getRoomId());
        assertEquals("host-1", room.getHostId());
        assertNull(room.getGuestId());
        assertEquals(RoomStatus.WAITING, room.getRoomStatus());
        assertEquals(GameType.CARO, room.getGameType());
        assertNotNull(room.getMatchEngine());
        assertNull(room.getMatchEngine().getCurrentBoard(), "MatchEngine chưa start nên currentBoard phải là null");
    }

    @Test
    @DisplayName("Tham gia phòng thành công: guestId cập nhật, roomStatus = PLAYING, matchEngine đã start (board không null)")
    void testJoinRoom_Success() {
        String roomId = roomManager.createRoom("host-1", GameType.CARO);

        Room joinedRoom = roomManager.joinRoom(roomId, "guest-1");

        assertNotNull(joinedRoom);
        assertEquals("guest-1", joinedRoom.getGuestId());
        assertEquals(RoomStatus.PLAYING, joinedRoom.getRoomStatus());
        assertNotNull(joinedRoom.getMatchEngine().getCurrentBoard(), "MatchEngine đã start nên board không null");
        assertEquals(MatchStatus.ONGOING, joinedRoom.getMatchEngine().getMatchStatus());
    }

    @Test
    @DisplayName("Tham gia phòng không tồn tại: ném RoomJoinException với thông báo 'Phòng không còn tồn tại'")
    void testJoinRoom_NotFound_ThrowsException() {
        RoomJoinException exception = assertThrows(
                RoomJoinException.class,
                () -> roomManager.joinRoom("non-existent-room-id", "guest-1")
        );

        assertEquals("Phòng không còn tồn tại", exception.getMessage());
    }

    @Test
    @DisplayName("Tham gia phòng đã đầy: ném RoomJoinException và người chơi thứ 3 không được ghi đè guestId của người chơi thứ 2")
    void testJoinRoom_RoomFull_ThrowsExceptionAndDoesNotOverwriteGuest() {
        String roomId = roomManager.createRoom("host-1", GameType.CARO);

        // Guest 1 tham gia thành công
        roomManager.joinRoom(roomId, "guest-1");

        // Guest 2 cố tham gia phòng đã có guest
        RoomJoinException exception = assertThrows(
                RoomJoinException.class,
                () -> roomManager.joinRoom(roomId, "guest-2")
        );

        assertEquals("Phòng đã đủ người hoặc ván đã bắt đầu", exception.getMessage());

        // Kiểm tra phòng vẫn giữ nguyên guest-1, không bị guest-2 ghi đè
        Room room = roomManager.getRoom(roomId).orElseThrow();
        assertEquals("guest-1", room.getGuestId(), "guestId không được bị ghi đè");
        assertEquals(RoomStatus.PLAYING, room.getRoomStatus());
    }

    @Test
    @DisplayName("Đồng thời nhiều luồng cùng joinRoom: chỉ duy nhất 1 luồng thành công, các luồng khác nhận exception")
    void testJoinRoom_ConcurrentAccess_ThreadSafe() throws InterruptedException {
        String roomId = roomManager.createRoom("host-1", GameType.GANH);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String guestId = "guest-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    roomManager.joinRoom(roomId, guestId);
                    successCount.incrementAndGet();
                } catch (RoomJoinException e) {
                    if ("Phòng đã đủ người hoặc ván đã bắt đầu".equals(e.getMessage())) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 luồng được join thành công");
        assertEquals(threadCount - 1, failCount.get(), "Các luồng còn lại phải bị từ chối");
        Room room = roomManager.getRoom(roomId).orElseThrow();
        assertNotNull(room.getGuestId());
        assertTrue(room.getGuestId().startsWith("guest-"));
        assertEquals(RoomStatus.PLAYING, room.getRoomStatus());
    }

    @Test
    @DisplayName("Disconnect khi phòng đang WAITING: Xóa luôn phòng và xóa playerToRoomMap")
    void testHandlePlayerDisconnect_WaitingRoom_RemovesRoomAndMapping() {
        String hostId = "host-disconnect-1";
        String roomId = roomManager.createRoom(hostId, GameType.CARO);

        assertEquals(Optional.of(roomId), roomManager.getRoomIdByPlayer(hostId));

        var disconnectOpt = roomManager.handlePlayerDisconnect(hostId);
        assertTrue(disconnectOpt.isPresent());
        assertEquals(roomId, disconnectOpt.get().roomId());
        assertEquals(RoomStatus.WAITING, disconnectOpt.get().previousStatus());
        assertEquals(hostId, disconnectOpt.get().disconnectedPlayerId());
        assertNull(disconnectOpt.get().opponentId());

        // Kiểm tra phòng đã bị xóa khỏi rooms
        assertTrue(roomManager.getRoom(roomId).isEmpty());
        // Kiểm tra playerToRoomMap đã xóa
        assertTrue(roomManager.getRoomIdByPlayer(hostId).isEmpty());
    }

    @Test
    @DisplayName("Disconnect khi phòng đang PLAYING: Xóa phòng, xóa mapping của cả 2 player, trả về opponentId")
    void testHandlePlayerDisconnect_PlayingRoom_RemovesRoomAndCleansBothPlayers() {
        String hostId = "host-disconnect-2";
        String guestId = "guest-disconnect-2";
        String roomId = roomManager.createRoom(hostId, GameType.CARO);
        roomManager.joinRoom(roomId, guestId);

        assertEquals(Optional.of(roomId), roomManager.getRoomIdByPlayer(hostId));
        assertEquals(Optional.of(roomId), roomManager.getRoomIdByPlayer(guestId));

        // Host disconnect
        var disconnectOpt = roomManager.handlePlayerDisconnect(hostId);
        assertTrue(disconnectOpt.isPresent());
        assertEquals(roomId, disconnectOpt.get().roomId());
        assertEquals(RoomStatus.PLAYING, disconnectOpt.get().previousStatus());
        assertEquals(hostId, disconnectOpt.get().disconnectedPlayerId());
        assertEquals(guestId, disconnectOpt.get().opponentId());

        // Cả phòng và cả 2 mapping đều đã bị xóa
        assertTrue(roomManager.getRoom(roomId).isEmpty());
        assertTrue(roomManager.getRoomIdByPlayer(hostId).isEmpty());
        assertTrue(roomManager.getRoomIdByPlayer(guestId).isEmpty());
    }

    @Test
    @DisplayName("Disconnect khi player không thuộc phòng nào: trả về Optional.empty()")
    void testHandlePlayerDisconnect_UnknownPlayer_ReturnsEmpty() {
        var disconnectOpt = roomManager.handlePlayerDisconnect("unknown-player");
        assertTrue(disconnectOpt.isEmpty());
    }

    @Test
    @DisplayName("Xóa mapping khi ván kết thúc bình thường: removePlayerMappings xóa đúng cả 2 người")
    void testRemovePlayerMappings_CleansBothPlayers() {
        String hostId = "host-finish";
        String guestId = "guest-finish";
        String roomId = roomManager.createRoom(hostId, GameType.CARO);
        roomManager.joinRoom(roomId, guestId);

        roomManager.removePlayerMappings(hostId, guestId);
        assertTrue(roomManager.getRoomIdByPlayer(hostId).isEmpty());
        assertTrue(roomManager.getRoomIdByPlayer(guestId).isEmpty());
        // Phòng vẫn có thể tra cứu để xem kết quả
        assertTrue(roomManager.getRoom(roomId).isPresent());
    }

    @Test
    @DisplayName("Chống spam tạo phòng (Race Condition): nhiều luồng cùng gọi createRoom với CÙNG 1 playerId đồng thời, chỉ đúng 1 luồng thành công")
    void testCreateRoom_ConcurrentSpamSamePlayer_ThreadSafe() throws InterruptedException {
        String playerId = "spam-player-1";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    roomManager.createRoom(playerId, GameType.CARO);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 luồng được tạo phòng thành công");
        assertEquals(threadCount - 1, failCount.get(), "Các luồng còn lại phải bị từ chối với IllegalStateException");
        assertTrue(roomManager.getRoomIdByPlayer(playerId).isPresent());
    }

    @Test
    @DisplayName("Dọn Zombie Room: removeRoom xóa hoàn toàn phòng khỏi memory")
    void testRemoveRoom_CleansFromMemory() {
        String roomId = roomManager.createRoom("host-zombie", GameType.CARO);
        assertTrue(roomManager.getRoom(roomId).isPresent());

        roomManager.removeRoom(roomId);
        assertTrue(roomManager.getRoom(roomId).isEmpty());
    }

    @Test
    @DisplayName("Chặn người chơi đang ở trong phòng khác join thêm phòng mới")
    void testJoinRoom_PlayerAlreadyInRoom_ThrowsIllegalStateException() {
        String host1 = "host-1-unique";
        String host2 = "host-2-unique";
        String player = "player-moving";

        String room1 = roomManager.createRoom(host1, GameType.CARO);
        String room2 = roomManager.createRoom(host2, GameType.CARO);

        roomManager.joinRoom(room1, player);

        assertThrows(
                IllegalStateException.class,
                () -> roomManager.joinRoom(room2, player),
                "Phải chặn người chơi join vào phòng thứ 2 khi đang ở phòng 1"
        );
    }
}
