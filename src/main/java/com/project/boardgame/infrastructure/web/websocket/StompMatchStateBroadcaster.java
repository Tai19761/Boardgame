package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.ports.MatchStateBroadcaster;
import com.project.boardgame.infrastructure.web.websocket.dto.GameStateResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter STOMP cho cổng {@link MatchStateBroadcaster}: gửi {@link GameStateResponse} tới
 * {@code /topic/match/{roomId}} — cùng topic với luồng nước đi của người chơi.
 */
@Component
public class StompMatchStateBroadcaster implements MatchStateBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public StompMatchStateBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastState(String roomId, Room room) {
        messagingTemplate.convertAndSend("/topic/match/" + roomId, GameStateResponse.from(roomId, room));
    }
}
