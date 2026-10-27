package dev.rinchan.paperplane;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A paper payment belongs to one native FTB teleporter until it completes or cancels. */
public final class TeleportPayments {
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private TeleportPayments() {}

    static boolean pending(UUID player) {
        return PENDING.containsKey(player);
    }

    static void begin(ServerPlayer player, PlaneKind kind, Object nativeTeleporter) {
        PENDING.put(player.getUUID(), new Pending(kind, nativeTeleporter));
    }

    public static <T> T teleport(ServerPlayer player, Object nativeTeleporter, Supplier<T> operation,
                          Predicate<T> successful, Supplier<T> missingPlane) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.nativeTeleporter() != nativeTeleporter) return operation.get();
        if (!PaperPlane.hasPlane(player, pending.kind())) {
            failed(player.getUUID());
            return missingPlane.get();
        }

        ItemStack payment = pending.kind().consumable() && !player.getAbilities().instabuild
                ? PaperPlane.takePlane(player, pending.kind()) : ItemStack.EMPTY;
        T result;
        try {
            result = operation.get();
        } catch (RuntimeException | Error failure) {
            PaperPlane.refund(player, payment);
            failed(player.getUUID());
            throw failure;
        }
        if (successful.test(result)) PaperPlane.finishTeleport(player, pending.kind());
        else {
            PaperPlane.refund(player, payment);
            failed(player.getUUID());
        }
        return result;
    }

    static void finish(UUID player) {
        PENDING.remove(player);
    }

    static void failed(UUID player) {
        PENDING.remove(player);
        PaperPlane.reconcileRequest(player);
    }

    public static void cancel(UUID player) {
        if (PENDING.remove(player) != null) PaperPlane.discardPendingRequest(player);
    }

    public static void reconcile(Set<UUID> warmups, Set<UUID> additions) {
        for (UUID player : Set.copyOf(PENDING.keySet())) {
            if (!warmups.contains(player) && !additions.contains(player)) cancel(player);
        }
    }

    private record Pending(PlaneKind kind, Object nativeTeleporter) {}
}
