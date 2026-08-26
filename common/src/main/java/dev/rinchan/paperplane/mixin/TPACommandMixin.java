package dev.rinchan.paperplane.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ftb.mods.ftbessentials.command.TPACommands;
import dev.rinchan.paperplane.PaperPlane;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TPACommands.class, remap = false)
public abstract class TPACommandMixin {
    @WrapMethod(method = "tpaccept")
    private static int paperPlane$acceptWithAtomicPayment(
        ServerPlayer target,
        String requestId,
        Operation<Integer> original
    ) {
        return PaperPlane.acceptTeleportRequest(target, requestId, () -> original.call(target, requestId));
    }

    @Inject(method = "tpdeny", at = @At("RETURN"), remap = false)
    private static void paperPlane$forgetDeniedRequest(
        ServerPlayer target,
        String requestId,
        CallbackInfoReturnable<Integer> callback
    ) {
        PaperPlane.forgetDeniedRequest(target, requestId, callback.getReturnValueI());
    }
}
