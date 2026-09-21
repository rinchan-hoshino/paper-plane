package dev.rinchan.paperplane.entity;

import dev.rinchan.paperplane.PaperPlaneFlightModel;
import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PaperPlaneEntity extends ThrowableItemProjectile {
    private boolean enderPlane;
    private boolean resolved;

    public PaperPlaneEntity(EntityType<? extends PaperPlaneEntity> type, Level level) {
        super(type, level);
    }

    public PaperPlaneEntity(Level level, LivingEntity owner, boolean enderPlane) {
        super(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), owner, level);
        this.enderPlane = enderPlane;
    }

    @Override
    public void tick() {
        if (resolved) {
            return;
        }
        Vec3 current = getDeltaMovement();
        PaperPlaneFlightModel.Velocity next = PaperPlaneFlightModel.step(
            new PaperPlaneFlightModel.Velocity(current.x, current.y, current.z)
        );
        setDeltaMovement(next.x(), next.y(), next.z());
        orientToVelocity();
        super.tick();
        if (!level().isClientSide() && !resolved && !isRemoved() && isInWater()) {
            resolveDrop(true);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide() && !resolved) {
            resolveDrop(false);
        }
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    protected Item getDefaultItem() {
        return PaperPlaneRegistries.PAPER_PLANE.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("EnderPlane", enderPlane);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enderPlane = tag.getBoolean("EnderPlane");
    }

    private void orientToVelocity() {
        Vec3 velocity = getDeltaMovement();
        double horizontal = velocity.horizontalDistance();
        if (horizontal < 1.0E-6D && Math.abs(velocity.y) < 1.0E-6D) {
            return;
        }
        setYRot((float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        setXRot((float) (-Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG));
    }

    private void resolveDrop(boolean wet) {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!enderPlane) {
            ItemStack result = new ItemStack(wet ? PaperPlaneRegistries.SOGGY_PAPER_PLANE.get() : PaperPlaneRegistries.PAPER_PLANE.get());
            spawnAtLocation(result);
        }
        discard();
    }
}
