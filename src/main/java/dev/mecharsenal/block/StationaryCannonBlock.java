package dev.mecharsenal.block;

import com.mojang.serialization.MapCodec;
import dev.mecharsenal.registry.ModBlocks;
import dev.mecharsenal.weapon.AutocannonService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class StationaryCannonBlock extends Block {
    public static final MapCodec<StationaryCannonBlock> CODEC = simpleCodec(StationaryCannonBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public StationaryCannonBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        for (StationaryCannonPartBlock.Part part : StationaryCannonPartBlock.Part.values()) {
            BlockPos partPos = StationaryCannonPartBlock.getPartPos(pos, facing, part);
            if (context.getLevel().isOutsideBuildHeight(partPos)
                    || !context.getLevel().getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction facing = state.getValue(FACING);
        for (StationaryCannonPartBlock.Part part : StationaryCannonPartBlock.Part.values()) {
            BlockPos partPos = StationaryCannonPartBlock.getPartPos(pos, facing, part);
            level.setBlock(partPos, ModBlocks.STATIONARY_CANNON_PART.get()
                    .defaultBlockState()
                    .setValue(StationaryCannonPartBlock.FACING, facing)
                    .setValue(StationaryCannonPartBlock.PART, part), 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            Direction facing = state.getValue(FACING);
            for (StationaryCannonPartBlock.Part part : StationaryCannonPartBlock.Part.values()) {
                BlockPos partPos = StationaryCannonPartBlock.getPartPos(pos, facing, part);
                if (level.getBlockState(partPos).is(ModBlocks.STATIONARY_CANNON_PART.get())) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static Vec3 muzzlePosition(BlockPos anchor, Direction facing) {
        BlockPos frontTop = anchor.relative(facing, 2).above();
        return Vec3.atCenterOf(frontTop).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.52));
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
            AutocannonService.fireStationary(serverPlayer, serverLevel, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
