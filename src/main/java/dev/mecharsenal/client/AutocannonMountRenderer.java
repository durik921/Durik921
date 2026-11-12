package dev.mecharsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mecharsenal.block.entity.AutocannonMountBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class AutocannonMountRenderer extends GeoBlockRenderer<AutocannonMountBlockEntity> {
    public AutocannonMountRenderer(BlockEntityRendererProvider.Context context) {
        super(new AutocannonTurretModel());
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
    }

    @Override
    public AABB getRenderBoundingBox(AutocannonMountBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0, 2.0, 3.0);
    }

    @Override
    public boolean shouldRenderOffScreen(AutocannonMountBlockEntity blockEntity) {
        return true;
    }
}
