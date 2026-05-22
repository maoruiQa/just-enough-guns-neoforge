package ttv.migami.jeg.vehicle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.util.HudMessageHelper;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.block.property.BlockPart;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;

public final class VehicleAssemblingTableBlock extends BaseEntityBlock {
    public static final MapCodec<VehicleAssemblingTableBlock> CODEC = simpleCodec(VehicleAssemblingTableBlock::new);
    public static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BlockPart> BLOCK_PART = EnumProperty.create("block_part", BlockPart.class);

    public VehicleAssemblingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BLOCK_PART, BlockPart.FLB));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPart part = state.getValue(BLOCK_PART);
        if (part == BlockPart.FLU || part == BlockPart.FRU || part == BlockPart.BLU || part == BlockPart.BRU) {
            return Block.box(0, 0, 0, 16, 14, 16);
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        Direction facing = state.getValue(FACING);
        BlockPos initialPos = null;
        for (BlockPart part : BlockPart.values()) {
            BlockPos blockPos = part.relativeNegative(pos, facing);
            if (VehicleAssemblingTableBlockItem.canPlace(level, blockPos, facing, pos)) {
                initialPos = blockPos;
                break;
            }
        }

        if (initialPos == null) {
            return;
        }

        for (BlockPart part : BlockPart.values()) {
            BlockPos blockPos = part.relative(initialPos, facing);
            level.setBlock(blockPos, state.setValue(BLOCK_PART, part), 3);
            level.sendBlockUpdated(initialPos, Blocks.AIR.defaultBlockState(), state, 3);
            state.updateNeighbourShapes(level, initialPos, 3);
        }
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState state, LevelReader levelReader, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        BlockPos originalPos = state.getValue(BLOCK_PART).relativeNegative(pos, facing);

        for (BlockPart part : BlockPart.values()) {
            BlockPos relativePos = part.relative(originalPos, facing);
            if (!relativePos.equals(neighborPos)) {
                continue;
            }
            if (neighborState.getBlock() != this || neighborState.getValue(BLOCK_PART) != part) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, levelReader, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            Direction facing = state.getValue(FACING);
            BlockPart part = state.getValue(BLOCK_PART);
            BlockPos originalPos = part.relativeNegative(pos, facing);

            for (BlockPart blockPart : BlockPart.values()) {
                BlockPos blockPos = blockPart.relative(originalPos, facing);
                BlockState blockState = level.getBlockState(blockPos);
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!Config.vehiclesEnabled()) {
            if (!level.isClientSide()) {
                HudMessageHelper.showActionBar(player, Component.translatable("message.jeg.vehicle.disabled"));
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof VehicleAssemblingTableBlockEntity table) {
            serverPlayer.openMenu(table, buffer -> buffer.writeBlockPos(pos));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BLOCK_PART);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new VehicleAssemblingTableBlockEntity(pos, state);
    }
}
