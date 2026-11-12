package dev.mecharsenal.client;

import dev.mecharsenal.MechArsenal;
import dev.mecharsenal.block.entity.AutocannonMountBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class AutocannonTurretModel extends GeoModel<AutocannonMountBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "geo/autocannon_turret.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "textures/block/autocannon_turret.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MechArsenal.MOD_ID, "animations/autocannon_turret.animation.json");

    @Override
    public ResourceLocation getModelResource(AutocannonMountBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AutocannonMountBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AutocannonMountBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(AutocannonMountBlockEntity animatable, long instanceId,
                                   software.bernie.geckolib.animation.AnimationState<AutocannonMountBlockEntity> animationState) {
        GeoBone yawBone = getAnimationProcessor().getBone("yaw");
        GeoBone pitchBone = getAnimationProcessor().getBone("pitch");
        if (yawBone == null || pitchBone == null) {
            throw new IllegalStateException("Autocannon geo model is missing its yaw or pitch bone");
        }
        yawBone.setRotY(animatable.getAimYaw());
        pitchBone.setRotX(animatable.getAimPitch());
    }
}
