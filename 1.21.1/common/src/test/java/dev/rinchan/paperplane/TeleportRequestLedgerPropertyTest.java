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
        for (long seed = 0; seed < 1_024; seed++) {
            verifyGeneratedTrace(new ProductionLedger(), seed, 160);
        }
    }

    @Test
    void multiPlayerSessionLedgerAndPaymentStateMachineHasNoCrossWiringOrDoubleDeduction() {
        List<UUID> players = List.of(
            new UUID(10L, 1L),
            new UUID(10L, 2L),
            new UUID(10L, 3L),
            new UUID(10L, 4L),
            new UUID(10L, 5L),
            new UUID(10L, 6L)
        );
        PlaneKind[] tpaKinds = {PlaneKind.NORMAL, PlaneKind.ENDER};

        for (long seed = 1L; seed <= 1_024; seed++) {
            Random rng = new Random(seed * 0x517CC1B727220A95L);
            SelectionSessions sessions = new SelectionSessions();
            TeleportRequestLedger ledger = new TeleportRequestLedger();

            // Per-player inventory of planes and active warmup payments
            Map<UUID, Map<PlaneKind, Integer>> inventory = new HashMap<>();
            Map<UUID, Map<PlaneKind, Integer>> totalConsumed = new HashMap<>();
            Map<UUID, UUID> activeSessionToken = new HashMap<>();
            Map<UUID, PlaneKind> activeWarmup = new HashMap<>();
            Map<UUID, UUID> activeWarmupRequestId = new HashMap<>();
            Map<UUID, TeleportRequestLedger.Pending> ftbRequests = new LinkedHashMap<>();
            List<UUID> allRequestIds = new ArrayList<>();
            java.util.Set<UUID> completedRequestIds = new java.util.HashSet<>();
            long nextReqSeq = 1L;
            long now = 0L;

            for (UUID p : players) {
                Map<PlaneKind, Integer> inv = new java.util.EnumMap<>(PlaneKind.class);
                Map<PlaneKind, Integer> cons = new java.util.EnumMap<>(PlaneKind.class);
                for (PlaneKind k : tpaKinds) {
                    inv.put(k, 8);
                    cons.put(k, 0);
                }
                inventory.put(p, inv);
                totalConsumed.put(p, cons);
            }

            for (int step = 0; step < 100; step++) {
                String ctx = "seed=" + seed + ", step=" + step + ", now=" + now;
                UUID actor = players.get(rng.nextInt(players.size()));
                int op = rng.nextInt(8);

                switch (op) {
                    case 0 -> {
                        // Open selection session
                        PlaneKind kind = tpaKinds[rng.nextInt(tpaKinds.length)];
                        if (!ledger.hasPending(actor) && !activeWarmup.containsKey(actor) && inventory.get(actor).get(kind) > 0) {
                            UUID token = sessions.open(actor, kind, now);
                            activeSessionToken.put(actor, token);
                        }
                    }
                    case 1 -> {
                        // Submit RequestTeleportPacket (sometimes with stale/wrong token or self target)
                        UUID token = rng.nextBoolean()
                            ? activeSessionToken.getOrDefault(actor, UUID.randomUUID())
                            : activeSessionToken.getOrDefault(players.get(rng.nextInt(players.size())), UUID.randomUUID());
                        UUID target = players.get(rng.nextInt(players.size()));
                        Optional<PlaneKind> consumed = sessions.consume(actor, token, now);
                        if (consumed.isPresent()) {
                            PlaneKind kind = consumed.get();
                            if (!ledger.hasPending(actor)
                                && !activeWarmup.containsKey(actor)
                                && !target.equals(actor)
                                && inventory.get(actor).get(kind) > 0) {
                                UUID reqId = new UUID(seed, nextReqSeq++);
                                TeleportRequestLedger.Pending p = new TeleportRequestLedger.Pending(reqId, actor, target, kind);
                                assertEquals(true, ledger.add(p), ctx + ": fresh request must enter ledger");
                                ftbRequests.put(reqId, p);
                                allRequestIds.add(reqId);
                            }
                        }
                    }
                    case 2 -> {
                        // Accept request (actor may be rightful target OR malicious third party)
                        if (!allRequestIds.isEmpty()) {
                            UUID reqId = allRequestIds.get(rng.nextInt(allRequestIds.size()));
                            Optional<TeleportRequestLedger.Pending> found = ledger.byRequest(reqId);
                            if (found.isPresent()) {
                                TeleportRequestLedger.Pending p = found.get();
                                if (!ledger.isOwnedByTarget(reqId, actor)) {
                                    // Non-target MUST NOT trigger accept or consume requester's plane
                                    assertEquals(false, p.targetId().equals(actor), ctx);
                                } else if (!activeWarmup.containsKey(p.requesterId())
                                    && inventory.get(p.requesterId()).get(p.planeKind()) > 0
                                    && ftbRequests.containsKey(reqId)) {
                                    // Rightful target starts warmup (plane not yet deducted until completion)
                                    ftbRequests.remove(reqId);
                                    activeWarmup.put(p.requesterId(), p.planeKind());
                                    activeWarmupRequestId.put(p.requesterId(), reqId);
                                }
                            }
                        }
                    }
                    case 3 -> {
                        // Deny request (actor may be rightful target OR malicious third party)
                        if (!allRequestIds.isEmpty()) {
                            UUID reqId = allRequestIds.get(rng.nextInt(allRequestIds.size()));
                            Optional<TeleportRequestLedger.Pending> found = ledger.byRequest(reqId);
                            if (found.isPresent() && found.get().targetId().equals(actor)) {
                                // Only if not already in warmup
                                if (!reqId.equals(activeWarmupRequestId.get(found.get().requesterId()))) {
                                    ledger.remove(reqId);
                                    ftbRequests.remove(reqId);
                                }
                            }
                        }
                    }
                    case 4 -> {
                        // Complete warmup (succeeds or fails/refunds)
                        PlaneKind kind = activeWarmup.remove(actor);
                        UUID reqId = activeWarmupRequestId.remove(actor);
                        if (kind != null) {
                            boolean succeeded = rng.nextInt(4) != 0;
                            int before = inventory.get(actor).get(kind);
                            if (succeeded && before > 0) {
                                // Atomic deduction ONLY on success for consumable planes
                                if (kind.consumable()) {
                                    inventory.get(actor).put(kind, before - 1);
                                    totalConsumed.get(actor).merge(kind, 1, Integer::sum);
                                }
                                assertEquals(true, completedRequestIds.add(reqId), ctx + ": request completed twice (double deduction!)");
                                ledger.remove(reqId);
                            } else {
                                // Failed teleport -> refund (net 0 deduction) & reconcile
                                ledger.remove(reqId);
                            }
                        }
                    }
                    case 5 -> {
                        // Cancel warmup (movement interruption -> 0 deduction)
                        UUID reqId = activeWarmupRequestId.remove(actor);
                        if (activeWarmup.remove(actor) != null && reqId != null) {
                            ledger.remove(reqId);
                        }
                    }
                    case 6 -> {
                        // Logout: clear session, warmup, and all ledger requests involving actor
                        sessions.remove(actor);
                        activeSessionToken.remove(actor);
                        activeWarmup.remove(actor);
                        activeWarmupRequestId.remove(actor);
                        for (TeleportRequestLedger.Pending removed : ledger.removePlayer(actor)) {
                            ftbRequests.remove(removed.requestId());
                        }
                    }
                    case 7 -> {
                        // Advance server clock (tests session expiration)
                        now += rng.nextBoolean() ? 200L : 700L;
                    }
                    default -> throw new AssertionError();
                }

                // Global invariants after EVERY single state transition:
                for (UUID p : players) {
                    // 1. Ender planes NEVER decrease
                    assertEquals(8, inventory.get(p).get(PlaneKind.ENDER), ctx + ": ender plane was consumed!");
                    assertEquals(0, totalConsumed.get(p).get(PlaneKind.ENDER), ctx + ": ender plane consumption counter non-zero!");
                    // 2. Normal plane accounting exact: remaining + consumed == initial (8)
                    int rem = inventory.get(p).get(PlaneKind.NORMAL);
                    int cons = totalConsumed.get(p).get(PlaneKind.NORMAL);
                    assertEquals(8, rem + cons, ctx + ": plane conservation violated for " + p);
                    // 3. Ledger bijective consistency (no orphan locks / no cross-wiring)
                    Optional<TeleportRequestLedger.Pending> pending = ledger.pendingFor(p);
                    assertEquals(pending.isPresent(), ledger.hasPending(p), ctx);
                    pending.ifPresent(entry -> {
                        assertEquals(p, entry.requesterId(), ctx + ": requester mismatch (cross-wiring!)");
                        assertEquals(Optional.of(entry), ledger.byRequest(entry.requestId()), ctx + ": byRequest mismatch");
                        assertEquals(true, ledger.isOwnedByTarget(entry.requestId(), entry.targetId()), ctx);
                        for (UUID other : players) {
                            if (!other.equals(entry.targetId())) {
                                assertEquals(false, ledger.isOwnedByTarget(entry.requestId(), other), ctx + ": wrong target owns request!");
                            }
                        }
                    });
                }
            }
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
