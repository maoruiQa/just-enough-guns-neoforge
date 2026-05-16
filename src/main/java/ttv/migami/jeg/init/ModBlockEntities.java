package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.block.entity.VehicleChargingStationBlockEntity;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleContainerBlockEntity>> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            () -> BlockEntityType.Builder.of(VehicleContainerBlockEntity::new, ModBlocks.VEHICLE_CONTAINER.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleAssemblingTableBlockEntity>> VEHICLE_ASSEMBLING_TABLE = REGISTER.register(
            "vehicle_assembling_table",
            () -> BlockEntityType.Builder.of(VehicleAssemblingTableBlockEntity::new, ModBlocks.VEHICLE_ASSEMBLING_TABLE.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleChargingStationBlockEntity>> VEHICLE_CHARGING_STATION = REGISTER.register(
            "vehicle_charging_station",
            () -> BlockEntityType.Builder.of(VehicleChargingStationBlockEntity::new, ModBlocks.VEHICLE_CHARGING_STATION.get()).build(null)
    );
}
