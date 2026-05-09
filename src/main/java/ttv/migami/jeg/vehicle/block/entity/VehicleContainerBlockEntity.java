package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.entity.TestWheelVehicleEntity;

public final class VehicleContainerBlockEntity extends BlockEntity {
    public VehicleContainerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_CONTAINER.get(), pos, blockState);
    }

    public boolean deploy(Player player) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }
        BlockPos spawnPos = this.getBlockPos().above();
        if (!level.getBlockState(spawnPos).isAir()) {
            player.displayClientMessage(Component.translatable("message.jeg.vehicle_container.blocked"), true);
            return false;
        }

        TestWheelVehicleEntity vehicle = ModEntities.TEST_WHEEL_VEHICLE.get().create(level);
        if (vehicle == null) {
            return false;
        }
        vehicle.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        level.addFreshEntity(vehicle);
        level.removeBlock(this.getBlockPos(), false);
        return true;
    }
}
