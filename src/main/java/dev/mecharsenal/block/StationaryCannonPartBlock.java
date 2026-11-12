package dev.mecharsenal.block;

import com.mojang.serialization.MapCodec;
import dev.mecharsenal.registry.ModBlocks;
import dev.mecharsenal.weapon.AutocannonService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class StationaryCannonPartBlock extends Block {
    public static final MapCodec<StationaryCannonPartBlock> CODEC = simpleCodec(StationaryCannonPartBlock::new);
    public static final DirectionProperty FACING = StationaryCannonBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public StationaryCannonPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.REAR_TOP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.is(ModBlocks.STATIONARY_CANNON_PART.get())) {
            BlockPos anchor = getAnchorPos(pos, state);
            if (level.getBlockState(anchor).is(ModBlocks.STATIONARY_CANNON.get())) {
                level.destroyBlock(anchor, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            AutocannonService.fireStationary(serverPlayer, serverLevel, getAnchorPos(pos, state));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public static BlockPos getAnchorPos(BlockPos partPos, BlockState state) {
        Direction back = state.getValue(FACING).getOpposite();
        return switch (state.getValue(PART)) {
            case REAR_TOP -> partPos.below();
            case MIDDLE_TOP -> partPos.below().relative(back);
            case FRONT_TOP -> partPos.below().relative(back, 2);
        };
    }

    public static BlockPos getPartPos(BlockPos anchor, Direction facing, Part part) {
        return switch (part) {
            case REAR_TOP -> anchor.above();
            case MIDDLE_TOP -> anchor.relative(facing).above();
            case FRONT_TOP -> anchor.relative(facing, 2).above();
        };
    }

    public enum Part implements StringRepresentable {
        REAR_TOP("rear_top"),
        MIDDLE_TOP("middle_top"),
        FRONT_TOP("front_top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
