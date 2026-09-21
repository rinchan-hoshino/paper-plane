package dev.rinchan.paperplane.mixin;

import dev.rinchan.paperplane.client.PaperPlaneClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void paperPlane$handleTrackedTeleportResponse(Style style, CallbackInfoReturnable<Boolean> callback) {
        ClickEvent click = style == null ? null : style.getClickEvent();
        if (click != null
            && click.getAction() == ClickEvent.Action.RUN_COMMAND
            && click.getValue().startsWith("/")
            && PaperPlaneClient.handleTeleportResponseCommand(click.getValue().substring(1))) {
            callback.setReturnValue(true);
        }
    }
}
