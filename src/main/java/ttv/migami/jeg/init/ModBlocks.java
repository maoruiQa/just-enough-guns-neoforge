package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.VehicleContainerBlock;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(Registries.BLOCK, Reference.MOD_ID);

    public static final DeferredHolder<Block, VehicleContainerBlock> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            () -> new VehicleContainerBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion())
    );
}
