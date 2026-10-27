package dev.rinchan.paperplane.mixin;

import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import dev.rinchan.paperplane.TeleportPayments;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WarmupCooldownTeleporter.class, remap = false)
public class NativeWarmupMixin {
    @Shadow @Final private static Map<UUID, ?> WARMUPS;
    @Shadow @Final private static Map<UUID, ?> pendingAdditions;

    @Shadow @Final private static Set<UUID> pendingRemovals;

    @Inject(method = "tickWarmups", at = @At("RETURN"))
    private static void paperPlane$clearInterrupted(MinecraftServer server, CallbackInfo ci) {
        TeleportPayments.reconcile(WARMUPS.keySet(), pendingAdditions.keySet());
    }

    @Inject(method = "cancelWarmup", at = @At("RETURN"))
    private static void paperPlane$cancel(ServerPlayer player, CallbackInfo ci) {
        if (pendingRemovals.contains(player.getUUID())) TeleportPayments.cancel(player.getUUID());
    }
}
