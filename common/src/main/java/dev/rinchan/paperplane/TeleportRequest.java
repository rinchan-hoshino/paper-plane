package dev.rinchan.paperplane;

import java.util.UUID;

public record TeleportRequest(UUID id, UUID requesterId, UUID targetId, boolean enderPlane, int remainingTicks) {
    public TeleportRequest tick() {
        return new TeleportRequest(id, requesterId, targetId, enderPlane, remainingTicks - 1);
    }
}
