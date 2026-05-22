package ttv.migami.jeg.vehicle.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.vehicle.block.VehicleAssemblingTableBlock;
import ttv.migami.jeg.vehicle.block.property.BlockPart;

public final class VehicleAssemblingTableBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VehicleAssemblingTableBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static @Nullable BlockPos findInitialPos(@NotNull BlockPlaceContext context, BlockPos currentPos, Direction facing) {
        for (BlockPart part : BlockPart.values()) {
            BlockPos placePos = part.relativeNegative(currentPos, facing);
            if (canPlace(context, placePos, facing)) {
                return placePos;
            }
        }
        return null;
    }

    @Override
    protected boolean canPlace(@NotNull BlockPlaceContext context, @NotNull BlockState state) {
        if (!Config.vehiclesEnabled()) {
            var player = context.getPlayer();
            if (!context.getLevel().isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.jeg.vehicle.disabled"), true);
            }
            return false;
        }
        Direction facing = state.getValue(VehicleAssemblingTableBlock.FACING);
        BlockPos initialPos = findInitialPos(context, context.getClickedPos(), facing);
        if (initialPos == null) {
            return false;
        }

        var player = context.getPlayer();
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (BlockPart blockPart : BlockPart.values()) {
            BlockPos blockPos = blockPart.relative(initialPos, facing);
            if (!context.getLevel().isUnobstructed(state, blockPos, collisionContext)) {
                return false;
            }
        }

        return super.canPlace(context, state);
    }

    public static boolean canPlace(@NotNull BlockPlaceContext context, BlockPos pos, Direction direction) {
        for (BlockPart part : BlockPart.values()) {
            BlockPos detectPos = part.relative(pos, direction);
            if (!context.getLevel().getBlockState(detectPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canPlace(@NotNull Level level, BlockPos pos, Direction direction, BlockPos skipPos) {
        for (BlockPart part : BlockPart.values()) {
            BlockPos detectPos = part.relative(pos, direction);
            if (detectPos.equals(skipPos)) {
                continue;
            }
            if (!level.getBlockState(detectPos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
