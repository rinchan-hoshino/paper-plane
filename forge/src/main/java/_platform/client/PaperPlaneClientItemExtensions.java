package dev.rinchan.paperplane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.mojang.math.Axis;

public final class PaperPlaneClientItemExtensions implements IClientItemExtensions {
    public static final PaperPlaneClientItemExtensions INSTANCE = new PaperPlaneClientItemExtensions();

    private PaperPlaneClientItemExtensions() {
    }

    @Override
    public boolean applyForgeHandTransform(
        PoseStack poseStack,
        LocalPlayer player,
        HumanoidArm arm,
        ItemStack stack,
        float partialTick,
        float equipProcess,
        float swingProcess
    ) {
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return false;
        }
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * 0.18F, 0.12F, -0.42F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side > 0.0F ? -225.0F : -45.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 5.0F));
        return true;
    }
}
