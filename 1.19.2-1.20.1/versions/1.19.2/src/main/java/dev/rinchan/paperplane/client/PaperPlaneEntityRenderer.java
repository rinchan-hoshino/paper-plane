package dev.rinchan.paperplane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PaperPlaneEntityRenderer extends EntityRenderer<PaperPlaneEntity> {
    private final ItemRenderer itemRenderer;

    public PaperPlaneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PaperPlaneEntity plane, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        float interpolatedYaw = Mth.rotLerp(partialTick, plane.yRotO, plane.getYRot());
        float interpolatedPitch = Mth.lerp(partialTick, plane.xRotO, plane.getXRot());
        poseStack.mulPose(Vector3f.YP.rotationDegrees(interpolatedYaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(interpolatedPitch));
        poseStack.scale(0.8F, 0.8F, 0.8F);
        itemRenderer.renderStatic(
            plane.getItem(),
            ItemTransforms.TransformType.GROUND,
            light,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffers,
            plane.getId()
        );
        poseStack.popPose();
        super.render(plane, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PaperPlaneEntity plane) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
