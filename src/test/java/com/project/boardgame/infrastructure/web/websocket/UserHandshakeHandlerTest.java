package com.project.boardgame.infrastructure.web.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserHandshakeHandlerTest {

    @Test
    @DisplayName("UserHandshakeHandler gán Principal UUID không null và duy nhất cho mỗi kết nối")
    void testDetermineUser_GeneratesValidUuidPrincipal() {
        UserHandshakeHandler handler = new UserHandshakeHandler();

        Principal user1 = handler.determineUser(null, null, new HashMap<>());
        Principal user2 = handler.determineUser(null, null, new HashMap<>());

        assertNotNull(user1);
        assertNotNull(user2);
        assertNotNull(user1.getName());
        assertNotNull(user2.getName());
        assertNotEquals(user1.getName(), user2.getName(), "Mỗi kết nối phải được gán UUID khác nhau");

        // Kiểm tra đúng định dạng UUID
        assertDoesNotThrow(() -> UUID.fromString(user1.getName()));
        assertDoesNotThrow(() -> UUID.fromString(user2.getName()));
    }
}
