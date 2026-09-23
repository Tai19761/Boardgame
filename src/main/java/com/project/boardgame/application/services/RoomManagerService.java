package com.project.boardgame.application.services;

import com.project.boardgame.application.models.GameType;
import com.project.boardgame.application.models.Room;
import com.project.boardgame.application.models.RoomStatus;
import com.project.boardgame.application.ports.GameRuleEngineFactory;
import com.project.boardgame.domain.rules.GameRuleEngine;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManagerService {

    private final GameRuleEngineFactory ruleEngineFactory;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomManagerService(GameRuleEngineFactory ruleEngineFactory) {
        this.ruleEngineFactory = ruleEngineFactory;
    }

    public String createRoom(String playerId, GameType gameType) {
        GameRuleEngine engine = ruleEngineFactory.create(gameType);
        MatchEngineService matchEngine = new MatchEngineService();
        Room room = new Room(playerId, gameType, engine, matchEngine);
        rooms.put(room.getRoomId(), room);
        return room.getRoomId();
    }

    public Room joinRoom(String roomId, String playerId) {
        Room joinedRoom = rooms.computeIfPresent(roomId, (id, room) -> {
            if (room.getRoomStatus() != RoomStatus.WAITING || room.getGuestId() != null) {
                throw new RoomJoinException("Room is full or already started");
            }
            room.joinGuest(playerId);
            return room;
        });

        if (joinedRoom == null) {
            throw new RoomJoinException("Room not found");
        }

        return joinedRoom;
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
}
