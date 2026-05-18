package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), toOutput(tag));
        return stack;
    }

    public static ItemStack createDefaultItem() {
        return createItemForVehicle(Reference.id("test_wheel_vehicle"));
    }

    public static ItemStack createItemForVehicle(Identifier vehicleId) {
        ItemStack stack = new ItemStack(ModItems.VEHICLE_CONTAINER.get());
        CompoundTag tag = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        var data = ttv.migami.jeg.vehicle.data.VehicleDataManager.get(vehicleId);
        tag.putString(TAG_ENTITY_TYPE, data.defaults().entityType());
        entityTag.putString(TAG_VEHICLE_ID, vehicleId.toString());
        entityTag.putInt(TAG_ENERGY, initialDeploymentEnergy(data.defaults().maxEnergy()));
        tag.put(TAG_ENTITY, entityTag);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.VEHICLE_CONTAINER.get(), toOutput(tag));
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos spawnPos = this.getBlockPos().above();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(this.entityType));
        Entity entity = type.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (!(entity instanceof VehicleEntity vehicle)) {
            return false;
        }
        if (!this.hasDeploymentSpace(level, type)) {
            player.sendSystemMessage(Component.translatable("message.jeg.vehicle_container.blocked"));
            return false;
        }
        if (this.entityTag != null) {
            vehicle.loadVehicleContainerState(this.entityTag);
        }
        double spawnX = spawnPos.getX() + 0.5D;
        double spawnY = spawnPos.getY() + vehicle.getBbHeight() * 0.5D;
        double spawnZ = spawnPos.getZ() + 0.5D;
        vehicle.snapTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);
        vehicle.setYRot(player.getYRot());
        vehicle.yRotO = vehicle.getYRot();
        vehicle.refreshDimensions();
        if (!level.noCollision(vehicle, vehicle.getBoundingBox())) {
            player.sendSystemMessage(Component.translatable("message.jeg.vehicle_container.blocked"));
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
        components.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(ModBlockEntities.VEHICLE_CONTAINER.get(), this.saveDataToTag()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.entityType = input.getStringOr(TAG_ENTITY_TYPE, this.entityType);
        this.entityTag = input.read(TAG_ENTITY, CompoundTag.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.saveDataToOutput(output);
    }

    private CompoundTag saveDataToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ENTITY_TYPE, this.entityType);
        if (this.entityTag != null) {
            tag.put(TAG_ENTITY, this.entityTag.copy());
        }
        return tag;
    }

    private void saveDataToOutput(ValueOutput output) {
        output.putString(TAG_ENTITY_TYPE, this.entityType);
        if (this.entityTag != null) {
            output.store(TAG_ENTITY, CompoundTag.CODEC, this.entityTag);
        }
    }

    private static TagValueOutput toOutput(CompoundTag tag) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.putString(TAG_ENTITY_TYPE, tag.getStringOr(TAG_ENTITY_TYPE, ""));
        tag.getCompound(TAG_ENTITY).ifPresent(entityTag -> output.store(TAG_ENTITY, CompoundTag.CODEC, entityTag));
        return output;
    }
}
