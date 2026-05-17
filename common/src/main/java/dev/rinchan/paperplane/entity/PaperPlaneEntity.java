package dev.rinchan.paperplane.entity;

import dev.rinchan.paperplane.registry.PaperPlaneRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class PaperPlaneEntity extends ThrowableItemProjectile {
    private boolean enderPlane;

    public PaperPlaneEntity(EntityType<? extends PaperPlaneEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PaperPlaneEntity(Level level, LivingEntity owner, boolean enderPlane) {
        super(PaperPlaneRegistries.PAPER_PLANE_ENTITY.get(), owner, level);
        this.enderPlane = enderPlane;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isInWater()) {
            dropResult(true);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            dropResult(false);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.025D;
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

    private void dropResult(boolean wet) {
        if (!enderPlane) {
            ItemStack drop = new ItemStack(wet ? PaperPlaneRegistries.SOGGY_PAPER_PLANE.get() : PaperPlaneRegistries.PAPER_PLANE.get());
            spawnAtLocation(drop);
        }
        discard();
    }
}
