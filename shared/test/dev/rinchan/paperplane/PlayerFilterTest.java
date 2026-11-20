package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PlayerFilterTest {
    private static final List<String> PLAYERS = List.of("Alex", "Steve", "RinChan", "alex_builder");

    @Test
    void blankQueryReturnsAllPlayersInOrder() {
        assertEquals(PLAYERS, PlayerFilter.filter(PLAYERS, s -> s, ""));
        assertEquals(PLAYERS, PlayerFilter.filter(PLAYERS, s -> s, "   "));
        assertEquals(PLAYERS, PlayerFilter.filter(PLAYERS, s -> s, null));
    }

    @Test
    void queryMatchesCaseInsensitivelyAndTrimsWhitespace() {
        assertEquals(List.of("Alex", "alex_builder"), PlayerFilter.filter(PLAYERS, s -> s, "  ALEX "));
        assertEquals(List.of("RinChan"), PlayerFilter.filter(PLAYERS, s -> s, "chan"));
        assertEquals(List.of(), PlayerFilter.filter(PLAYERS, s -> s, "notch"));
    }

    @Test
    void uniqueMatchReturnsOnlyWhenExactlyOnePlayerMatches() {
        assertEquals(Optional.of("RinChan"), PlayerFilter.uniqueMatch(PLAYERS, s -> s, "rin"));
        assertTrue(PlayerFilter.uniqueMatch(PLAYERS, s -> s, "alex").isEmpty());
        assertTrue(PlayerFilter.uniqueMatch(PLAYERS, s -> s, "nobody").isEmpty());
    }
}
