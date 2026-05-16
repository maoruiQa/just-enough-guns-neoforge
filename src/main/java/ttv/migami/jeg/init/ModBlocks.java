package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.VehicleAssemblingTableBlock;
import ttv.migami.jeg.vehicle.block.VehicleChargingStationBlock;
import ttv.migami.jeg.vehicle.block.VehicleContainerBlock;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(Registries.BLOCK, Reference.MOD_ID);

    public static final DeferredHolder<Block, VehicleContainerBlock> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            id -> new VehicleContainerBlock(blockProperties(id).strength(3.0F).sound(SoundType.METAL).noOcclusion())
    );

    public static final DeferredHolder<Block, VehicleAssemblingTableBlock> VEHICLE_ASSEMBLING_TABLE = REGISTER.register(
            "vehicle_assembling_table",
            id -> new VehicleAssemblingTableBlock(blockProperties(id).strength(2.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion())
    );

    public static final DeferredHolder<Block, VehicleChargingStationBlock> VEHICLE_CHARGING_STATION = REGISTER.register(
            "vehicle_charging_station",
            id -> new VehicleChargingStationBlock(blockProperties(id).strength(3.5F).sound(SoundType.METAL))
    );

    private static BlockBehaviour.Properties blockProperties(Identifier id) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
    }
}
