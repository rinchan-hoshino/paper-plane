package dev.rinchan.paperplane.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.TPACommand;
import dev.rinchan.paperplane.PaperPlane;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TPACommand.class, remap = false)
public class TPACommandMixin {
    @WrapMethod(method = "tpaccept")
    private int paperPlane$acceptWithAtomicPayment(ServerPlayer target, String requestId, Operation<Integer> original) {
        return PaperPlane.acceptTeleportRequest(target, requestId, () -> original.call(target, requestId));
    }

    @Inject(method = "tpdeny", at = @At("RETURN"), remap = false)
    private void paperPlane$forgetDeniedRequest(ServerPlayer target, String requestId, CallbackInfoReturnable<Integer> cir) {
        UUID id = parseRequestId(requestId);
        if (id != null) {
            PaperPlane.forgetDeniedRequest(target, id, cir.getReturnValueI());
        }
    }

    private static UUID parseRequestId(String requestId) {
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
