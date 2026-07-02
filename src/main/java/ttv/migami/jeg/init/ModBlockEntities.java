package ttv.migami.jeg.init;

import java.lang.reflect.Proxy;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.block.entity.DynamicLightBlockEntity;
import ttv.migami.jeg.block.entity.MagazineLoaderBlockEntity;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.block.entity.VehicleChargingStationBlockEntity;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;

public final class ModBlockEntities {
    @FunctionalInterface
    private interface BlockEntityFactory<T extends net.minecraft.world.level.block.entity.BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamicLightBlockEntity>> DYNAMIC_LIGHT = REGISTER.register(
            "dynamic_light",
            () -> createBlockEntityType(DynamicLightBlockEntity::new, ModBlocks.DYNAMIC_LIGHT.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleContainerBlockEntity>> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            () -> createBlockEntityType(VehicleContainerBlockEntity::new, ModBlocks.VEHICLE_CONTAINER.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleAssemblingTableBlockEntity>> VEHICLE_ASSEMBLING_TABLE = REGISTER.register(
            "vehicle_assembling_table",
            () -> createBlockEntityType(VehicleAssemblingTableBlockEntity::new, ModBlocks.VEHICLE_ASSEMBLING_TABLE.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleChargingStationBlockEntity>> VEHICLE_CHARGING_STATION = REGISTER.register(
            "vehicle_charging_station",
            () -> createBlockEntityType(VehicleChargingStationBlockEntity::new, ModBlocks.VEHICLE_CHARGING_STATION.get())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagazineLoaderBlockEntity>> MAGAZINE_LOADER = REGISTER.register(
            "magazine_loader",
            () -> createBlockEntityType(MagazineLoaderBlockEntity::new, ModBlocks.MAGAZINE_LOADER.get())
    );

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> createBlockEntityType(
            BlockEntityFactory<T> factory,
            net.minecraft.world.level.block.Block block
    ) {
        try {
            Class<?> supplierType = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> factory.create((BlockPos) args[0], (BlockState) args[1])
            );
            var constructor = BlockEntityType.class.getDeclaredConstructor(supplierType, Set.class);
            constructor.setAccessible(true);
            return (BlockEntityType<T>) constructor.newInstance(supplier, Set.of(block));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create JEG block entity type", exception);
        }
    }
}
