package ttv.migami.jeg.vehicle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;

public final class VehicleContainerBlock extends BaseEntityBlock {
    public static final MapCodec<VehicleContainerBlock> CODEC = simpleCodec(VehicleContainerBlock::new);

    public VehicleContainerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModItems.CROWBAR.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof VehicleContainerBlockEntity container)) {
            return InteractionResult.FAIL;
        }
        if (!container.deploy(player)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new VehicleContainerBlockEntity(pos, state);
    }
}
