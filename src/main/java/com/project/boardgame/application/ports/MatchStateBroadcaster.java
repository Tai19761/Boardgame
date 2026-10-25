package com.project.boardgame.application.ports;

import com.project.boardgame.application.models.Room;

/**
 * Cổng (port) phát trạng thái ván đấu tới client. Tầng application chỉ biết interface này;
 * implementation (STOMP/WebSocket) nằm ở tầng infrastructure.
 */
public interface MatchStateBroadcaster {

    void broadcastState(String roomId, Room room);
}
