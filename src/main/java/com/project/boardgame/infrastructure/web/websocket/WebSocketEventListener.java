package com.project.boardgame.infrastructure.web.websocket;

import com.project.boardgame.application.models.PlayerDisconnectInfo;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.services.RoomManagerService;
import com.project.boardgame.infrastructure.web.websocket.dto.PlayerDisconnectedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Optional;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final RoomManagerService roomManagerService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(RoomManagerService roomManagerService, SimpMessagingTemplate messagingTemplate) {
        this.roomManagerService = roomManagerService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) {
            return;
        }

        String playerId = user.getName();
        log.info("Player disconnected: {}", playerId);

        Optional<PlayerDisconnectInfo> disconnectInfoOpt = roomManagerService.handlePlayerDisconnect(playerId);
        if (disconnectInfoOpt.isPresent()) {
            PlayerDisconnectInfo info = disconnectInfoOpt.get();
            if (info.previousStatus() == RoomStatus.PLAYING && info.opponentId() != null) {
                log.info("Notifying room {} of player {} disconnection", info.roomId(), playerId);
                messagingTemplate.convertAndSend(
                        "/topic/match/" + info.roomId(),
                        new PlayerDisconnectedResponse(info.roomId(), playerId)
                );
            }
        }
    }
}
