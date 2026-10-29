package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.services.AiMoveService;
import com.project.boardgame.application.services.InvalidMoveException;
import com.project.boardgame.application.services.MatchStatus;
import com.project.boardgame.application.services.RoomJoinException;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.application.services.RoomNotFoundException;
import com.project.boardgame.application.services.UnauthorizedSpectatorException;
import com.project.boardgame.domain.ai.AiDifficulty;
import com.project.boardgame.domain.models.Player;
import com.project.boardgame.infrastructure.web.websocket.dto.CreateRoomRequest;
import com.project.boardgame.infrastructure.web.websocket.dto.CreateRoomResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.ErrorResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.GameStateResponse;
import com.project.boardgame.infrastructure.web.websocket.dto.JoinRoomRequest;
import com.project.boardgame.infrastructure.web.websocket.dto.MoveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final RoomManagerService roomManagerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiMoveService aiMoveService;

    /**
     * Constructor cũ giữ lại cho các test hiện có (không dùng Bot). Không có AiMoveService thì
     * phòng đấu máy sẽ không được lên lịch nước đi của Bot.
     */
    public MatchController(RoomManagerService roomManagerService, SimpMessagingTemplate messagingTemplate) {
        this(roomManagerService, messagingTemplate, null);
    }

    @Autowired
    public MatchController(
            RoomManagerService roomManagerService,
            SimpMessagingTemplate messagingTemplate,
            AiMoveService aiMoveService
    ) {
        this.roomManagerService = roomManagerService;
        this.messagingTemplate = messagingTemplate;
        this.aiMoveService = aiMoveService;
    }

    @MessageMapping("/room/create")
    @SendToUser("/queue/room/created")
    public CreateRoomResponse createRoom(
            @Payload(required = false) CreateRoomRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (headerAccessor.getUser() == null) {
            throw new IllegalStateException("Phiên kết nối người dùng không hợp lệ");
        }
        String playerId = headerAccessor.getUser().getName();
        GameType gameType = (request != null && request.gameType() != null)
                ? request.gameType()
                : GameType.CARO;
        // Không tự validate: RoomManagerService là nguồn sự thật duy nhất cho whitelist boardSize
        Integer boardSize = request != null ? request.boardSize() : null;

        String roomId = roomManagerService.createRoom(playerId, gameType, boardSize);
        log.info("Player {} created room {} for game {} (boardSize={})", playerId, roomId, gameType, boardSize);
        return new CreateRoomResponse(roomId);
    }

    /**
     * Tạo phòng đấu với máy. Trả trạng thái ban đầu trực tiếp về user (ván đã ONGOING ngay),
     * vì client chỉ subscribe /topic/match/{roomId} sau khi biết roomId.
     */
    @MessageMapping("/room/create-bot")
    @SendToUser("/queue/room/bot-created")
    public GameStateResponse createBotRoom(
            @Payload(required = false) CreateRoomRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (headerAccessor.getUser() == null) {
            throw new IllegalStateException("Phiên kết nối người dùng không hợp lệ");
        }
        String playerId = headerAccessor.getUser().getName();
        GameType gameType = (request != null && request.gameType() != null)
                ? request.gameType()
                : GameType.CARO;
        Integer boardSize = request != null ? request.boardSize() : null;
        AiDifficulty difficulty = request != null ? request.difficulty() : null;

        Room room = roomManagerService.createBotRoom(playerId, gameType, boardSize, difficulty);
        log.info("Player {} created BOT room {} for game {} (boardSize={}, difficulty={})",
                playerId, room.getRoomId(), gameType, room.getBoardSize(), room.getDifficulty());
        synchronized (room) {
            return GameStateResponse.from(room.getRoomId(), room);
        }
    }

    @MessageMapping("/room/join")
    public void joinRoom(
            @Payload JoinRoomRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (headerAccessor.getUser() == null) {
            throw new IllegalStateException("Phiên kết nối người dùng không hợp lệ");
        }
        if (request == null || request.roomId() == null || request.roomId().isBlank()) {
            throw new IllegalArgumentException("Mã phòng không được để trống");
        }

        String playerId = headerAccessor.getUser().getName();
        String roomId = request.roomId();

        Room room = roomManagerService.joinRoom(roomId, playerId);
        log.info("Player {} joined room {}", playerId, roomId);

        GameStateResponse response = GameStateResponse.from(roomId, room);
        messagingTemplate.convertAndSend("/topic/match/" + roomId, response);
    }

    @MessageMapping("/match/{roomId}/move")
    public void handleMove(
            @DestinationVariable String roomId,
            @Payload MoveRequest moveRequest,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("Phiên kết nối người dùng không hợp lệ");
        }
        if (moveRequest == null) {
            throw new IllegalArgumentException("Nước đi không được để trống");
        }

        String playerId = principal.getName();
        Room room = roomManagerService.getRoom(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng: " + roomId));

        Player player;
        if (playerId.equals(room.getHostId())) {
            player = Player.PLAYER_1;
        } else if (playerId.equals(room.getGuestId())) {
            player = Player.PLAYER_2;
        } else {
            throw new UnauthorizedSpectatorException("Người ngoài không có quyền thực hiện nước đi");
        }

        GameStateResponse response;
        synchronized (room) {
            room.getMatchEngine().executeMove(moveRequest.toMoveStep(), player);
            response = GameStateResponse.from(roomId, room);
        }

        messagingTemplate.convertAndSend("/topic/match/" + roomId, response);

        // Dọn dẹp mapping và xóa phòng (chống Zombie Room) khi ván đấu kết thúc bình thường
        if (response.matchStatus() != MatchStatus.ONGOING) {
            room.finishRoom();
            roomManagerService.removePlayerMappings(room.getHostId(), room.getGuestId());
            roomManagerService.removeRoom(roomId);
            log.info("Match in room {} finished with status {}. Cleaned up player mappings and removed zombie room.", roomId, response.matchStatus());
            return;
        }

        // Đấu với máy: sau khi đã broadcast nước của người chơi (ngoài khối synchronized), lên lịch lượt Bot
        if (room.isBotMatch() && response.currentPlayer() == Player.PLAYER_2 && aiMoveService != null) {
            aiMoveService.scheduleBotMove(roomId);
        }
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state exception: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "ILLEGAL_STATE");
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "INVALID_ARGUMENT");
    }

    @MessageExceptionHandler(InvalidMoveException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleInvalidMoveException(InvalidMoveException ex) {
        log.warn("Invalid move exception: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "INVALID_MOVE");
    }

    @MessageExceptionHandler(UnauthorizedSpectatorException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleUnauthorizedSpectatorException(UnauthorizedSpectatorException ex) {
        log.warn("Unauthorized spectator exception: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "UNAUTHORIZED_SPECTATOR");
    }

    @MessageExceptionHandler(RoomNotFoundException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleRoomNotFoundException(RoomNotFoundException ex) {
        log.warn("Room not found exception: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "ROOM_NOT_FOUND");
    }

    @MessageExceptionHandler(RoomJoinException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleRoomJoinException(RoomJoinException ex) {
        log.warn("Room join exception: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "ROOM_JOIN_ERROR");
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleGenericException(Exception ex) {
        log.error("Unhandled exception in MatchController: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không xác định", "SYSTEM_ERROR");
    }
}
