package dev.rinchan.paperplane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.rinchan.paperplane.entity.PaperPlaneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public final class PaperPlaneEntityRenderer extends EntityRenderer<PaperPlaneEntity, PaperPlaneEntityRenderer.RenderState> {
    private final ItemModelResolver itemModelResolver;

    public PaperPlaneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(PaperPlaneEntity plane, RenderState state, float partialTick) {
        super.extractRenderState(plane, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, plane.yRotO, plane.getYRot());
        state.pitch = Mth.lerp(partialTick, plane.xRotO, plane.getXRot());
        itemModelResolver.updateForNonLiving(state.item, plane.getItem(), ItemDisplayContext.GROUND, plane);
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        poseStack.scale(0.8F, 0.8F, 0.8F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class RenderState extends ThrownItemRenderState {
        private float yaw;
        private float pitch;
    }
}
