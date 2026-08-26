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
        assertTrue(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID().toString(), requester, first, PlaneKind.NORMAL)));
        assertFalse(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID().toString(), requester, UUID.randomUUID(), PlaneKind.ENDER)));
        assertTrue(ledger.pendingFor(requester).isPresent());
    }

    @Test
    void acceptanceDenialOrDisconnectCanReleaseTheLock() {
        TeleportRequestLedger ledger = new TeleportRequestLedger();
        UUID requester = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        String request = UUID.randomUUID().toString();
        ledger.add(new TeleportRequestLedger.Pending(request, requester, target, PlaneKind.SOGGY));
        ledger.remove(request);
        assertFalse(ledger.hasPending(requester));
        assertTrue(ledger.add(new TeleportRequestLedger.Pending(UUID.randomUUID().toString(), requester, target, PlaneKind.NORMAL)));
        assertTrue(ledger.removePlayer(target).size() == 1);
        assertFalse(ledger.hasPending(requester));
    }
}
