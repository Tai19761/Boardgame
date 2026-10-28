package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.ports.MatchStateBroadcaster;
import com.project.boardgame.domain.ai.CaroMinimax;
import com.project.boardgame.domain.models.BoardState;
import com.project.boardgame.domain.models.MoveStep;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.domain.models.Position;
import com.project.boardgame.domain.rules.CaroRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Điều phối nước đi của Bot: đẩy việc tính toán Minimax (CPU-bound) vào thread pool riêng,
 * sau đó áp kết quả lên phòng một cách an toàn với race-condition.
 * <p>
 * Executor được inject là bean {@code ExecutorService} khai báo trong {@code AiWorkerConfig}.
 * Kiểu tham số dùng interface cha {@link Executor} để test có thể truyền {@code Runnable::run}
 * (chạy đồng bộ) mà không cần thread thật.
 */
@Service
public class AiMoveService {

    private static final Logger log = LoggerFactory.getLogger(AiMoveService.class);

    static final Player BOT_PLAYER = Player.PLAYER_2;

    private final RoomManagerService roomManagerService;
    private final MatchStateBroadcaster broadcaster;
    private final Executor aiExecutor;

    public AiMoveService(
            RoomManagerService roomManagerService,
            MatchStateBroadcaster broadcaster,
            @Qualifier("aiExecutorService") Executor aiExecutor
    ) {
        this.roomManagerService = roomManagerService;
        this.broadcaster = broadcaster;
        this.aiExecutor = aiExecutor;
    }

    /**
     * Lên lịch cho Bot đi 1 nước trong phòng {@code roomId}. Trả về ngay, việc tính toán chạy trong pool.
     */
    public void scheduleBotMove(String roomId) {
        aiExecutor.execute(() -> runBotTurn(roomId));
    }

    private void runBotTurn(String roomId) {
        try {
            // 1) Lấy phòng và chụp snapshot bàn cờ (BoardState là record bất biến) trong khóa
            Optional<Room> roomOpt = roomManagerService.getRoom(roomId);
            if (roomOpt.isEmpty()) {
                log.debug("Bỏ qua lượt Bot: phòng {} không tồn tại", roomId);
                return;
            }
            Room room = roomOpt.get();
            if (!room.isBotMatch() || room.getGameType() != GameType.CARO) {
                return;
            }

            BoardState snapshot;
            int historySizeAtSnapshot;
            synchronized (room) {
                if (!isBotTurn(room)) {
                    return;
                }
                snapshot = room.getMatchEngine().getCurrentBoard();
                historySizeAtSnapshot = room.getMatchEngine().getMoveHistory().size();
            }

            // 2) Tính toán NGOÀI khóa (CPU nặng) trên snapshot bất biến; rule engine lấy từ phòng (do factory tạo),
            //    kích thước bàn lấy từ phòng để evaluator/Minimax không bị kẹt ở 15x15
            int boardSize = room.getBoardSize() != null ? room.getBoardSize() : CaroRuleEngine.DEFAULT_BOARD_SIZE;
            Position bestMove = new CaroMinimax(room.getRuleEngine(), boardSize).findBestMove(snapshot, BOT_PLAYER);

            // 3) Có kết quả: lấy lại phòng theo roomId, bỏ qua nếu phòng đã bị xóa/thay thế hoặc đã kết thúc
            Optional<Room> latestOpt = roomManagerService.getRoom(roomId);
            if (latestOpt.isEmpty() || latestOpt.get() != room) {
                log.debug("Bỏ kết quả Bot: phòng {} đã bị xóa trong lúc tính toán", roomId);
                return;
            }

            MatchStatus statusAfterMove;
            synchronized (room) {
                if (room.getRoomStatus() == RoomStatus.FINISHED
                        || !isBotTurn(room)
                        || room.getMatchEngine().getMoveHistory().size() != historySizeAtSnapshot) {
                    log.debug("Bỏ kết quả Bot: trạng thái phòng {} đã thay đổi trong lúc tính toán", roomId);
                    return;
                }
                room.getMatchEngine().executeMove(new MoveStep(null, bestMove), BOT_PLAYER);
                statusAfterMove = room.getMatchEngine().getMatchStatus();
            }

            // Broadcast ngoài khóa để giải phóng lock trước khi thực hiện I/O mạng
            broadcaster.broadcastState(roomId, room);

            // 4) Ván kết thúc sau nước của Bot: dọn dẹp như luồng người chơi (chống Zombie Room)
            if (statusAfterMove != MatchStatus.ONGOING) {
                room.finishRoom();
                roomManagerService.removePlayerMappings(room.getHostId(), room.getGuestId());
                roomManagerService.removeRoom(roomId);
                log.info("Bot match in room {} finished with status {}. Room cleaned up.", roomId, statusAfterMove);
            }
        } catch (Exception e) {
            // Không để exception bị nuốt im lặng trong thread pool
            log.error("Lỗi khi xử lý lượt Bot trong phòng {}: {}", roomId, e.getMessage(), e);
        }
    }

    private boolean isBotTurn(Room room) {
        MatchEngineService engine = room.getMatchEngine();
        return room.getRoomStatus() == RoomStatus.PLAYING
                && engine.getMatchStatus() == MatchStatus.ONGOING
                && engine.getCurrentBoard() != null
                && engine.getCurrentBoard().currentPlayer() == BOT_PLAYER;
    }
}
