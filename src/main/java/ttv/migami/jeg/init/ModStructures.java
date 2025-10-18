package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.worldgen.structure.SkyShipArmadaStructure;

public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Reference.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Reference.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<SkyShipArmadaStructure>> SKY_SHIP_ARMADA =
            STRUCTURES.register("sky_ship_armada", () -> () -> SkyShipArmadaStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SKY_SHIP_ARMADA_PIECE =
            PIECES.register("sky_ship_armada_piece", () -> (StructurePieceType.StructureTemplateType) (manager, tag) -> new SkyShipArmadaStructure.SkyShipPiece(manager, tag));

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SKY_SHIP_GUARDIAN_PIECE =
            PIECES.register("sky_ship_guardian_piece", () -> (StructurePieceType.ContextlessType) SkyShipArmadaStructure.GuardianSpawnPiece::new);
}
