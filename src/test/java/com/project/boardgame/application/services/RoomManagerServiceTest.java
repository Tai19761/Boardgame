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
    @DisplayName("Tham gia phòng không tồn tại: ném RoomJoinException với thông báo 'Room not found'")
    void testJoinRoom_NotFound_ThrowsException() {
        RoomJoinException exception = assertThrows(
                RoomJoinException.class,
                () -> roomManager.joinRoom("non-existent-room-id", "guest-1")
        );

        assertEquals("Room not found", exception.getMessage());
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

        assertEquals("Room is full or already started", exception.getMessage());

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
                    if ("Room is full or already started".equals(e.getMessage())) {
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
}
