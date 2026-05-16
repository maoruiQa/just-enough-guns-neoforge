package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleContainerBlockEntity extends BlockEntity {
    private static final String TAG_ENTITY_TYPE = "EntityType";
    private static final String TAG_ENTITY = "Entity";
    private static final String TAG_VEHICLE_ID = "VehicleDataId";
    private static final String TAG_ENERGY = "Energy";

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
        return createItemForVehicle(Reference.id("test_wheel_vehicle"));
    }

    public static ItemStack createItemForVehicle(ResourceLocation vehicleId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_CONTAINER.get());
        CompoundTag tag = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        var data = ttv.migami.jeg.vehicle.data.VehicleDataManager.get(vehicleId);
        tag.putString(TAG_ENTITY_TYPE, data.defaults().entityType());
        entityTag.putString(TAG_VEHICLE_ID, vehicleId.toString());
        entityTag.putInt(TAG_ENERGY, initialDeploymentEnergy(data.defaults().maxEnergy()));
        tag.put(TAG_ENTITY, entityTag);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), tag);
        return stack;
    }

    private static int initialDeploymentEnergy(int maxEnergy) {
        if (maxEnergy <= 0) {
            return 0;
        }
        return (int) Math.ceil(maxEnergy * 0.1D);
    }

    public boolean deploy(Player player) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }
        BlockPos spawnPos = this.getBlockPos().above();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(this.entityType));
        Entity entity = type.create(level);
        if (!(entity instanceof VehicleEntity vehicle)) {
            return false;
        }
        if (!this.hasDeploymentSpace(level, type)) {
            player.displayClientMessage(Component.translatable("message.jeg.vehicle_container.blocked"), true);
            return false;
        }
        if (this.entityTag != null) {
            vehicle.loadVehicleContainerState(this.entityTag);
        }
        double spawnX = spawnPos.getX() + 0.5D;
        double spawnY = spawnPos.getY() + vehicle.getBbHeight() * 0.5D;
        double spawnZ = spawnPos.getZ() + 0.5D;
        vehicle.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);
        vehicle.setYRot(player.getYRot());
        vehicle.yRotO = vehicle.getYRot();
        vehicle.refreshDimensions();
        if (!level.noCollision(vehicle, vehicle.getBoundingBox())) {
            player.displayClientMessage(Component.translatable("message.jeg.vehicle_container.blocked"), true);
            return false;
        }
        level.addFreshEntity(vehicle);
        level.removeBlock(this.getBlockPos(), false);
        return true;
    }

    private boolean hasDeploymentSpace(Level level, EntityType<?> type) {
        int radius = (int) (type.getDimensions().width() / 2.0F + 1.0F);
        int height = (int) (type.getDimensions().height() + 1.0F);
        BlockPos origin = this.getBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockState state = level.getBlockState(origin.offset(x, y, z));
                    if (state.canOcclude() && !state.is(Blocks.SNOW)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(this.saveDataToTag()));
    }

    @Override
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        super.saveToItem(stack, registries);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), this.saveDataToTag());
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
        this.saveDataToTag(tag);
    }

    private CompoundTag saveDataToTag() {
        CompoundTag tag = new CompoundTag();
        this.saveDataToTag(tag);
        return tag;
    }

    private void saveDataToTag(CompoundTag tag) {
        tag.putString(TAG_ENTITY_TYPE, this.entityType);
        if (this.entityTag != null) {
            tag.put(TAG_ENTITY, this.entityTag);
        }
    }
}
