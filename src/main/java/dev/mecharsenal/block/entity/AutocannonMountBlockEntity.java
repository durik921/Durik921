package dev.mecharsenal.block.entity;

import dev.mecharsenal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class AutocannonMountBlockEntity extends BlockEntity implements GeoBlockEntity {
    private float aimYaw;
    private float aimPitch;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public AutocannonMountBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCANNON_MOUNT.get(), pos, state);
    }

    public void setAim(float yaw, float pitch) {
        yaw = wrapRadians(yaw);
        pitch = Math.max((float) Math.toRadians(-25.0), Math.min((float) Math.toRadians(45.0), pitch));
        if (Math.abs(wrapRadians(yaw - aimYaw)) < (float) Math.toRadians(0.75)
                && Math.abs(pitch - aimPitch) < (float) Math.toRadians(0.75)) {
            return;
        }

        aimYaw = yaw;
        aimPitch = pitch;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public Vec3 getAimDirection() {
        double cosPitch = Math.cos(aimPitch);
        return new Vec3(
                -Math.sin(aimYaw) * cosPitch,
                Math.sin(aimPitch),
                -Math.cos(aimYaw) * cosPitch
        );
    }

    public float getAimYaw() {
        return aimYaw;
    }

    public float getAimPitch() {
        return aimPitch;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("AimYaw", aimYaw);
        tag.putFloat("AimPitch", aimPitch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        aimYaw = wrapRadians(tag.getFloat("AimYaw"));
        aimPitch = tag.getFloat("AimPitch");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static float wrapRadians(float angle) {
        return (float) Math.atan2(Math.sin(angle), Math.cos(angle));
    }
}
