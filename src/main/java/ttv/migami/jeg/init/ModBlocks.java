package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.block.DynamicLightBlock;
import ttv.migami.jeg.block.MagazineLoaderBlock;
import ttv.migami.jeg.vehicle.block.VehicleAssemblingTableBlock;
import ttv.migami.jeg.vehicle.block.VehicleChargingStationBlock;
import ttv.migami.jeg.vehicle.block.VehicleContainerBlock;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(Registries.BLOCK, Reference.MOD_ID);

    public static final DeferredHolder<Block, DynamicLightBlock> DYNAMIC_LIGHT = REGISTER.register(
            "dynamic_light",
            DynamicLightBlock::new
    );

    public static final DeferredHolder<Block, VehicleContainerBlock> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            () -> new VehicleContainerBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion())
    );

    public static final DeferredHolder<Block, VehicleAssemblingTableBlock> VEHICLE_ASSEMBLING_TABLE = REGISTER.register(
            "vehicle_assembling_table",
            () -> new VehicleAssemblingTableBlock(BlockBehaviour.Properties.of().strength(2.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion())
    );

    public static final DeferredHolder<Block, VehicleChargingStationBlock> VEHICLE_CHARGING_STATION = REGISTER.register(
            "vehicle_charging_station",
            () -> new VehicleChargingStationBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL))
    );

    public static final DeferredHolder<Block, MagazineLoaderBlock> MAGAZINE_LOADER = REGISTER.register(
            "magazine_loader",
            () -> new MagazineLoaderBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL))
    );
}
