package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportRequestLedgerPropertyTest {
    private static final List<UUID> IDS = List.of(
        new UUID(0L, 1L),
        new UUID(0L, 2L),
        new UUID(0L, 3L),
        new UUID(0L, 4L),
        new UUID(0L, 5L)
    );

    @Test
    void generatedRequestSequencesPreserveBothIndexesAndTargetOwnership() {
        for (long seed = 0; seed < 128; seed++) {
            verifyGeneratedTrace(new ProductionLedger(), seed, 160);
        }
    }

    @Test
    void negativeFixtureRejectsRemovalThatLeavesTheRequesterLocked() {
        Ledger broken = new StaleRequesterLockLedger();
        ReferenceLedger reference = new ReferenceLedger();
        TeleportRequestLedger.Pending pending = pending(0, 1, 2, PlaneKind.NORMAL);

        applyAdd(reference, broken, pending, "add");
        assertThrows(AssertionError.class, () -> applyRemove(reference, broken, pending.requestId(), "remove"));
    }

    private static void verifyGeneratedTrace(Ledger actual, long seed, int operations) {
        ReferenceLedger expected = new ReferenceLedger();
        Random random = new Random(seed);
        for (int step = 0; step < operations; step++) {
            String context = "seed=" + seed + ", step=" + step;
            switch (random.nextInt(3)) {
                case 0 -> applyAdd(expected, actual, randomPending(random), context + ", op=add");
                case 1 -> applyRemove(expected, actual, randomId(random), context + ", op=remove");
                case 2 -> applyRemovePlayer(expected, actual, randomId(random), context + ", op=removePlayer");
                default -> throw new AssertionError("unreachable");
            }
        }
    }

    private static void applyAdd(
        ReferenceLedger expected,
        Ledger actual,
        TeleportRequestLedger.Pending pending,
        String context
    ) {
        assertEquals(expected.add(pending), actual.add(pending), context);
        assertEquivalent(expected, actual, context);
    }

    private static void applyRemove(ReferenceLedger expected, Ledger actual, UUID requestId, String context) {
        assertEquals(expected.remove(requestId), actual.remove(requestId), context);
        assertEquivalent(expected, actual, context);
    }

    private static void applyRemovePlayer(ReferenceLedger expected, Ledger actual, UUID playerId, String context) {
        assertEquals(expected.removePlayer(playerId), actual.removePlayer(playerId), context);
        assertEquivalent(expected, actual, context);
    }

    private static void assertEquivalent(ReferenceLedger expected, Ledger actual, String context) {
        for (UUID requestId : IDS) {
            assertEquals(expected.byRequest(requestId), actual.byRequest(requestId), context + ", requestId=" + requestId);
            for (UUID targetId : IDS) {
                assertEquals(
                    expected.isOwnedByTarget(requestId, targetId),
                    actual.isOwnedByTarget(requestId, targetId),
                    context + ", requestId=" + requestId + ", targetId=" + targetId
                );
            }
        }
        for (UUID requesterId : IDS) {
            assertEquals(
                expected.pendingFor(requesterId),
                actual.pendingFor(requesterId),
                context + ", requesterId=" + requesterId
            );
            assertEquals(
                expected.hasPending(requesterId),
                actual.hasPending(requesterId),
                context + ", requesterId=" + requesterId
            );
        }
    }

    private static TeleportRequestLedger.Pending randomPending(Random random) {
        return pending(
            random.nextInt(IDS.size()),
            random.nextInt(IDS.size()),
            random.nextInt(IDS.size()),
            PlaneKind.values()[random.nextInt(PlaneKind.values().length)]
        );
    }

    private static UUID randomId(Random random) {
        return IDS.get(random.nextInt(IDS.size()));
    }

    private static TeleportRequestLedger.Pending pending(
        int requestIndex,
        int requesterIndex,
        int targetIndex,
        PlaneKind kind
    ) {
        return new TeleportRequestLedger.Pending(
            IDS.get(requestIndex),
            IDS.get(requesterIndex),
            IDS.get(targetIndex),
            kind
        );
    }

    private interface Ledger {
        boolean add(TeleportRequestLedger.Pending pending);

        Optional<TeleportRequestLedger.Pending> pendingFor(UUID requesterId);

        Optional<TeleportRequestLedger.Pending> byRequest(UUID requestId);

        boolean isOwnedByTarget(UUID requestId, UUID targetId);

        boolean hasPending(UUID requesterId);

        Optional<TeleportRequestLedger.Pending> remove(UUID requestId);

        List<TeleportRequestLedger.Pending> removePlayer(UUID playerId);
    }

    private static final class ProductionLedger implements Ledger {
        private final TeleportRequestLedger delegate = new TeleportRequestLedger();

        @Override
        public boolean add(TeleportRequestLedger.Pending pending) {
            return delegate.add(pending);
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> pendingFor(UUID requesterId) {
            return delegate.pendingFor(requesterId);
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> byRequest(UUID requestId) {
            return delegate.byRequest(requestId);
        }

        @Override
        public boolean isOwnedByTarget(UUID requestId, UUID targetId) {
            return delegate.isOwnedByTarget(requestId, targetId);
        }

        @Override
        public boolean hasPending(UUID requesterId) {
            return delegate.hasPending(requesterId);
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> remove(UUID requestId) {
            return delegate.remove(requestId);
        }

        @Override
        public List<TeleportRequestLedger.Pending> removePlayer(UUID playerId) {
            return delegate.removePlayer(playerId);
        }
    }

    private static class ReferenceLedger implements Ledger {
        protected final Map<UUID, TeleportRequestLedger.Pending> byRequest = new LinkedHashMap<>();
        protected final Map<UUID, UUID> requestByRequester = new HashMap<>();

        @Override
        public boolean add(TeleportRequestLedger.Pending pending) {
            if (byRequest.containsKey(pending.requestId()) || requestByRequester.containsKey(pending.requesterId())) {
                return false;
            }
            byRequest.put(pending.requestId(), pending);
            requestByRequester.put(pending.requesterId(), pending.requestId());
            return true;
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> pendingFor(UUID requesterId) {
            UUID requestId = requestByRequester.get(requesterId);
            return requestId == null ? Optional.empty() : Optional.ofNullable(byRequest.get(requestId));
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> byRequest(UUID requestId) {
            return Optional.ofNullable(byRequest.get(requestId));
        }

        @Override
        public boolean isOwnedByTarget(UUID requestId, UUID targetId) {
            TeleportRequestLedger.Pending pending = byRequest.get(requestId);
            return pending != null && pending.targetId().equals(targetId);
        }

        @Override
        public boolean hasPending(UUID requesterId) {
            return requestByRequester.containsKey(requesterId);
        }

        @Override
        public Optional<TeleportRequestLedger.Pending> remove(UUID requestId) {
            TeleportRequestLedger.Pending pending = byRequest.remove(requestId);
            if (pending != null) {
                requestByRequester.remove(pending.requesterId(), requestId);
            }
            return Optional.ofNullable(pending);
        }

        @Override
        public List<TeleportRequestLedger.Pending> removePlayer(UUID playerId) {
            List<TeleportRequestLedger.Pending> removed = new ArrayList<>();
            for (TeleportRequestLedger.Pending pending : List.copyOf(byRequest.values())) {
                if (pending.requesterId().equals(playerId) || pending.targetId().equals(playerId)) {
                    remove(pending.requestId());
                    removed.add(pending);
                }
            }
            return removed;
        }
    }

    private static final class StaleRequesterLockLedger extends ReferenceLedger {
        @Override
        public Optional<TeleportRequestLedger.Pending> remove(UUID requestId) {
            return Optional.ofNullable(byRequest.remove(requestId));
        }
    }
}
