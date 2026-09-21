package dev.rinchan.paperplane;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SelectionSessionsTest {
    @Test void stalePacketCannotConsumeNewSelection() {
        SelectionSessions sessions = new SelectionSessions();
        UUID player = UUID.randomUUID();
        UUID old = sessions.open(player, PlaneKind.NORMAL, 0);
        UUID current = sessions.open(player, PlaneKind.ENDER, 1);
        assertTrue(sessions.consume(player, old, 2).isEmpty());
        assertEquals(PlaneKind.ENDER, sessions.consume(player, current, 2).orElseThrow());
        assertTrue(sessions.consume(player, current, 2).isEmpty());
    }
    @Test void tokenIsBoundToPlayerAndExpiresAtDeadline() {
        SelectionSessions sessions = new SelectionSessions();
        UUID player = UUID.randomUUID();
        UUID token = sessions.open(player, PlaneKind.NORMAL, 0);
        assertTrue(sessions.consume(UUID.randomUUID(), token, 10).isEmpty());
        assertTrue(sessions.consume(player, token, 600).isEmpty());
        token = sessions.open(player, PlaneKind.SOGGY, 600);
        assertEquals(PlaneKind.SOGGY, sessions.consume(player, token, 1199).orElseThrow());
    }
    @Test void disconnectRevokesSelection() {
        SelectionSessions sessions = new SelectionSessions();
        UUID player = UUID.randomUUID();
        UUID token = sessions.open(player, PlaneKind.NORMAL, 0);
        sessions.remove(player);
        assertTrue(sessions.consume(player, token, 1).isEmpty());
    }
}
