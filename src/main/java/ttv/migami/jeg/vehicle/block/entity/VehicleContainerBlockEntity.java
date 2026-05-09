package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.vehicle.entity.TestWheelVehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleContainerBlockEntity extends BlockEntity {
    private static final String TAG_ENTITY_TYPE = "EntityType";
    private static final String TAG_ENTITY = "Entity";

    private String entityType = "jeg:test_wheel_vehicle";
    private CompoundTag entityTag;

    public VehicleContainerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_CONTAINER.get(), pos, blockState);
    }

    public static ItemStack createItemFor(VehicleEntity vehicle) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_CONTAINER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).toString());
        tag.put(TAG_ENTITY, vehicle.saveVehicleContainerState());
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), tag);
        return stack;
    }

    public static ItemStack createDefaultItem() {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_CONTAINER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ENTITY_TYPE, "jeg:test_wheel_vehicle");
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), tag);
        return stack;
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
        if (this.entityTag != null) {
            vehicle.loadVehicleContainerState(this.entityTag);
        }
        vehicle.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        level.addFreshEntity(vehicle);
        level.removeBlock(this.getBlockPos(), false);
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ENTITY_TYPE)) {
            this.entityType = tag.getString(TAG_ENTITY_TYPE);
        }
        if (tag.contains(TAG_ENTITY)) {
            this.entityTag = tag.getCompound(TAG_ENTITY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_ENTITY_TYPE, this.entityType);
        if (this.entityTag != null) {
            tag.put(TAG_ENTITY, this.entityTag);
        }
    }
}
