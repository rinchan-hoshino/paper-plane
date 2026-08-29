package dev.rinchan.paperplane.mixin;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void paperPlane$handleTeleportResponse(String command, CallbackInfo callback) {
        if (PaperPlaneClient.handleTeleportResponseCommand(command)) {
            callback.cancel();
        }
    }
}
