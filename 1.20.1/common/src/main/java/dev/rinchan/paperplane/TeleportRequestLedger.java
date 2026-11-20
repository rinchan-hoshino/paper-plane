package dev.rinchan.paperplane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-owned one-request-per-requester state. */
public final class TeleportRequestLedger {
    private final Map<String, Pending> byRequest = new LinkedHashMap<>();
    private final Map<UUID, String> requestByRequester = new HashMap<>();

    public boolean add(Pending pending) {
        if (byRequest.containsKey(pending.requestId()) || requestByRequester.containsKey(pending.requesterId())) {
            return false;
        }
        byRequest.put(pending.requestId(), pending);
        requestByRequester.put(pending.requesterId(), pending.requestId());
        return true;
    }

    public Optional<Pending> pendingFor(UUID requesterId) {
        String requestId = requestByRequester.get(requesterId);
        return requestId == null ? Optional.empty() : Optional.ofNullable(byRequest.get(requestId));
    }

    public Optional<Pending> byRequest(String requestId) {
        return Optional.ofNullable(byRequest.get(requestId));
    }

    public boolean hasPending(UUID requesterId) {
        return requestByRequester.containsKey(requesterId);
    }

    public Optional<Pending> remove(String requestId) {
        Pending pending = byRequest.remove(requestId);
        if (pending != null) {
            requestByRequester.remove(pending.requesterId(), requestId);
        }
        return Optional.ofNullable(pending);
    }

    public List<Pending> removePlayer(UUID playerId) {
        List<Pending> removed = new ArrayList<>();
        for (Pending pending : List.copyOf(byRequest.values())) {
            if (pending.requesterId().equals(playerId) || pending.targetId().equals(playerId)) {
                remove(pending.requestId());
                removed.add(pending);
            }
        }
        return removed;
    }

    public record Pending(String requestId, UUID requesterId, UUID targetId, PlaneKind planeKind) {
        public Pending {
            if (requestId == null || requestId.isEmpty() || requesterId == null || targetId == null || planeKind == null) {
                throw new IllegalArgumentException("Pending request fields must be non-null and request id must be non-empty");
            }
        }
    }
}
