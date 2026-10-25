package dev.rinchan.paperplane;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-owned, one-use selection tokens. Stale packets cannot cancel a newer screen. */
public final class SelectionSessions {
    private static final long LIFETIME_TICKS = 20L * 30L;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public UUID open(UUID player, PlaneKind kind, long now) {
        Session active = sessions.get(player);
        // Holding use can queue several openings before the visible picker is clicked.
        // Reuse its authority rather than invalidating an otherwise valid selection.
        if (active != null && active.kind() == kind && now < active.expiresAt()) {
            return active.token();
        }
        UUID token = UUID.randomUUID();
        sessions.put(player, new Session(token, kind, now + LIFETIME_TICKS));
        return token;
    }

    public Optional<PlaneKind> consume(UUID player, UUID token, long now) {
        Session session = sessions.get(player);
        if (session == null || !session.token().equals(token)) {
            return Optional.empty();
        }
        sessions.remove(player);
        return now < session.expiresAt() ? Optional.of(session.kind()) : Optional.empty();
    }

    public void remove(UUID player) {
        sessions.remove(player);
    }

    private record Session(UUID token, PlaneKind kind, long expiresAt) {}
}
