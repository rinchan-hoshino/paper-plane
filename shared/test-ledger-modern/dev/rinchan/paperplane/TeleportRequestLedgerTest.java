package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportRequestLedgerTest {
    @Test
    void oneRequesterCanOwnOnlyOnePendingTarget() {
        TeleportRequestLedger ledger = new TeleportRequestLedger();
        UUID requester = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        assertTrue(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID(), requester, first, PlaneKind.NORMAL)));
        assertFalse(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID(), requester, UUID.randomUUID(), PlaneKind.ENDER)));
        assertTrue(ledger.pendingFor(requester).isPresent());
    }

    @Test
    void acceptanceDenialOrDisconnectCanReleaseTheLock() {
        TeleportRequestLedger ledger = new TeleportRequestLedger();
        UUID requester = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        ledger.add(new TeleportRequestLedger.Pending(request, requester, target, PlaneKind.SOGGY));
        ledger.remove(request);
        assertFalse(ledger.hasPending(requester));
        assertTrue(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID(), requester, target, PlaneKind.NORMAL)));
        assertTrue(ledger.removePlayer(target).size() == 1);
        assertFalse(ledger.hasPending(requester));
    }
}
