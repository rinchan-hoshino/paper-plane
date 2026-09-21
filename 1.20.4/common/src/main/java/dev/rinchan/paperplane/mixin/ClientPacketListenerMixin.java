package dev.rinchan.paperplane.mixin;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "sendUnsignedCommand", at = @At("HEAD"), cancellable = true)
    private void paperPlane$handleTeleportResponse(String command, CallbackInfoReturnable<Boolean> callback) {
        if (PaperPlaneClient.handleTeleportResponseCommand(command)) {
            callback.setReturnValue(true);
        }
    }
}
