package ttv.migami.jeg.worldgen.structure;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.DifficultyInstance;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantomGuardian;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.util.LootUtils;
import net.minecraft.world.level.storage.loot.LootTable;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.event.MainThreadLevelActionScheduler;

public class SkyShipArmadaStructure extends Structure {
    public static final MapCodec<SkyShipArmadaStructure> CODEC = simpleCodec(SkyShipArmadaStructure::new);

    private static final ResourceLocation TEMPLATE_LOCATION = ResourceLocation.withDefaultNamespace("end_city/ship");
    private static final int MIN_SHIPS = 8;
    private static final int MAX_SHIPS = 12;
    private static final int MIN_RADIUS = 18;
    private static final int MAX_RADIUS = 30;
    private static final double MIN_SHIP_SPACING = 28.0D;
    private static final int HEIGHT_OFFSET = 112;
    private static final int MIN_HEIGHT = 128;
    private static final int MAX_HEIGHT = 224;
    private static final int TERRAIN_SAMPLE_RADIUS = 40;
    private static final int TERRAIN_STEP = 8;
    private static final int MAX_TERRAIN_HEIGHT = 100;
    private static final int REQUIRED_LOOT_SHIPS_MIN = 2;
    private static final int REQUIRED_LOOT_SHIPS_MAX = 2;
    private static final int GUARDIAN_TETHER_RADIUS = 96;
    private static final ResourceLocation LOOT_TABLE = Reference.id("chests/sky_ship_armada");
    private static final ResourceLocation DAMAGED_LOOT_TABLE = Reference.id("chests/sky_ship_armada_damaged");
    private static final ResourceKey<LootTable> LOOT_TABLE_KEY = ResourceKey.create(Registries.LOOT_TABLE, LOOT_TABLE);
    private static final ResourceKey<LootTable> DAMAGED_LOOT_TABLE_KEY = ResourceKey.create(Registries.LOOT_TABLE, DAMAGED_LOOT_TABLE);

    public SkyShipArmadaStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        BlockPos chunkCenter = context.chunkPos().getMiddleBlockPosition(0);

        int maxTerrain = sampleTerrain(chunkGenerator, heightAccessor, chunkCenter, context);
        if (maxTerrain > MAX_TERRAIN_HEIGHT) {
            return Optional.empty();
        }

        int targetY = Math.max(MIN_HEIGHT, maxTerrain + HEIGHT_OFFSET);
        targetY = Math.min(targetY, MAX_HEIGHT);
        targetY = Math.min(targetY, chunkGenerator.getGenDepth() - 32);

        BlockPos anchor = new BlockPos(chunkCenter.getX(), targetY, chunkCenter.getZ());
        return Optional.of(new Structure.GenerationStub(anchor, builder -> generatePieces(builder, context, anchor)));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SKY_SHIP_ARMADA.get();
    }

    private int sampleTerrain(ChunkGenerator generator, LevelHeightAccessor accessor, BlockPos origin, GenerationContext context) {
        int maxHeight = Integer.MIN_VALUE;
        int minHeight = Integer.MAX_VALUE;
        for (int dx = -TERRAIN_SAMPLE_RADIUS; dx <= TERRAIN_SAMPLE_RADIUS; dx += TERRAIN_STEP) {
            for (int dz = -TERRAIN_SAMPLE_RADIUS; dz <= TERRAIN_SAMPLE_RADIUS; dz += TERRAIN_STEP) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, accessor, context.randomState());
                if (height > maxHeight) {
                    maxHeight = height;
                }
                if (height < minHeight) {
                    minHeight = height;
                }
            }
        }
        if (maxHeight == Integer.MIN_VALUE) {
            return origin.getY();
        }

        if (maxHeight - minHeight > 20) {
            return MAX_TERRAIN_HEIGHT + 1;
        }

        return maxHeight;
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos anchor) {
        RandomSource random = context.random();
        StructureTemplateManager templateManager = context.structureTemplateManager();

        int shipCount = Mth.nextInt(random, MIN_SHIPS, MAX_SHIPS);
        float baseAngle = random.nextFloat() * Mth.TWO_PI;

        int maxLootShips = Math.min(REQUIRED_LOOT_SHIPS_MAX, shipCount);
        int minLootShips = Math.min(REQUIRED_LOOT_SHIPS_MIN, shipCount);
        int lootShips = Mth.nextInt(random, minLootShips, maxLootShips);

        List<Boolean> lootAssignments = new ArrayList<>(shipCount);
        for (int i = 0; i < shipCount; i++) {
            lootAssignments.add(i < lootShips);
        }
        java.util.Collections.shuffle(lootAssignments, new java.util.Random(random.nextLong()));

        List<BlockPos> occupied = new ArrayList<>();
        for (int i = 0; i < shipCount; i++) {
            boolean hasLoot = lootAssignments.get(i);
            BlockPos shipPos = null;
            float angle = baseAngle + (float) (i * (2.0F * Math.PI / shipCount));

            for (int attempt = 0; attempt < 8 && shipPos == null; attempt++) {
                float attemptAngle = angle + (random.nextFloat() - 0.5F) * 0.8F;
                double distance = Mth.nextDouble(random, MIN_RADIUS + 4.0D, MAX_RADIUS + 8.0D);
                int offsetX = Mth.floor(Math.cos(attemptAngle) * distance);
                int offsetZ = Mth.floor(Math.sin(attemptAngle) * distance);
                BlockPos candidate = anchor.offset(offsetX, 0, offsetZ);
                if (!isTooClose(candidate, occupied)) {
                    shipPos = candidate;
                }
            }

            if (shipPos == null) {
                double fallbackDistance = MAX_RADIUS + 12.0D + i * 2.5D;
                int offsetX = Mth.floor(Math.cos(baseAngle + i) * fallbackDistance);
                int offsetZ = Mth.floor(Math.sin(baseAngle + i) * fallbackDistance);
                shipPos = anchor.offset(offsetX, 0, offsetZ);
            }

            occupied.add(shipPos);
            boolean damagedElytra = hasLoot;
            SkyShipPiece piece = new SkyShipPiece(templateManager, shipPos, Rotation.getRandom(random), damagedElytra, hasLoot);
            builder.addPiece(piece);
        }

        builder.addPiece(new GuardianSpawnPiece(anchor));
    }

    private boolean isTooClose(BlockPos candidate, List<BlockPos> occupied) {
        for (BlockPos pos : occupied) {
            if (pos.distSqr(candidate) < MIN_SHIP_SPACING * MIN_SHIP_SPACING) {
                return true;
            }
        }
        return false;
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setRotationPivot(new BlockPos(0, 0, 0))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(new RuleProcessor(List.of(
                        new ProcessorRule(new BlockMatchTest(Blocks.DRAGON_HEAD), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                        new ProcessorRule(new BlockMatchTest(Blocks.DRAGON_WALL_HEAD), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState())
                )))
                .setIgnoreEntities(true);
    }

    public static class SkyShipPiece extends TemplateStructurePiece {
        private static final String DAMAGE_FLAG = "DamagedElytra";
        private static final String LOOT_FLAG = "HasLoot";
        private final boolean damagedElytra;
        private final boolean hasLootChest;
        private final Set<BlockPos> elytraMarkers = new LinkedHashSet<>();

        public SkyShipPiece(StructureTemplateManager manager, BlockPos pos, Rotation rotation, boolean damagedElytra, boolean hasLootChest) {
            super(ModStructures.SKY_SHIP_ARMADA_PIECE.get(), 0, manager, TEMPLATE_LOCATION, TEMPLATE_LOCATION.toString(), makeSettings(rotation), pos);
            this.damagedElytra = damagedElytra;
            this.hasLootChest = hasLootChest;
            // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Created ship piece at {} hasLoot={} damagedElytra={}", pos, hasLootChest, damagedElytra);
        }

        public SkyShipPiece(StructureTemplateManager manager, CompoundTag nbt) {
            super(ModStructures.SKY_SHIP_ARMADA_PIECE.get(), nbt, manager, location -> makeSettings(Rotation.valueOf(nbt.contains("Rot") ? nbt.getString("Rot") : "NONE")));
            this.hasLootChest = nbt.contains(LOOT_FLAG) ? nbt.getBoolean(LOOT_FLAG) : true;
            this.damagedElytra = nbt.contains(DAMAGE_FLAG) ? nbt.getBoolean(DAMAGE_FLAG) : this.hasLootChest;
        }

        @Override
        public void postProcess(
            WorldGenLevel level,
            StructureManager manager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos blockPos
        ) {
            super.postProcess(level, manager, generator, random, box, chunkPos, blockPos);
            if (this.hasLootChest && this.damagedElytra) {
                placeDamagedElytra(level, random);
            }
            if (!this.hasLootChest) {
                carveDamagedSections(level, box, random);
            }
        }

        @Override
        protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
            if ("Chest".equals(marker)) {
                BlockPos chestPos = pos.below();
                if (!box.isInside(chestPos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return;
                }

                BlockState currentState = level.getBlockState(chestPos);
                if (!(currentState.getBlock() instanceof ChestBlock)) {
                    Direction facing = this.getRotation().rotate(Direction.NORTH);
                    BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
                    level.setBlock(chestPos, chestState, 3);
                }

                if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity)) {
                    BlockState chestState = level.getBlockState(chestPos);
                    var created = BlockEntityType.CHEST.create(chestPos, chestState);
                    if (created != null) {
                        level.getLevel().setBlockEntity(created);
                    }
                }

                LootUtils.fillContainer(level, chestPos, hasLootChest ? LOOT_TABLE_KEY : DAMAGED_LOOT_TABLE_KEY, random);
                var blockEntity = level.getBlockEntity(chestPos);
                if (blockEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity randomizable) {
                    ResourceKey<net.minecraft.world.level.storage.loot.LootTable> assigned = randomizable.getLootTable();
                    String lootId = assigned != null ? assigned.location().toString() : "none";
                    // ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Chest {} lootTable={} seed={}", chestPos, lootId, randomizable.getLootTableSeed());
                } else if (blockEntity instanceof net.minecraft.world.Container container) {
                    boolean empty = LootUtils.isContainerEmpty(container);
                    // ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Chest {} filled empty={}", chestPos, empty);
                } else {
                    // ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Chest {} has no container block entity", chestPos);
                }
            } else if ("Elytra".equals(marker)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (!hasLootChest) {
                    return;
                }
                this.elytraMarkers.add(pos.immutable());
                // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Recorded Elytra marker {} (lootShip={} hasLoot={})", pos, this.damagedElytra, hasLootChest);
            } else if ("Sentry".equals(marker)) {
                placeGunnerSpawner(level, pos, random);
            }
        }

        private void placeGunnerSpawner(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
            if (!this.boundingBox.isInside(pos)) {
                return;
            }

            if (!isInteriorSpawnerPosition(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                return;
            }

            BlockState spawnerState = Blocks.SPAWNER.defaultBlockState();
            level.setBlock(pos, spawnerState, 18);
            if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity)) {
                var created = BlockEntityType.MOB_SPAWNER.create(pos, spawnerState);
                if (created != null) {
                    level.getLevel().setBlockEntity(created);
                }
            }

            if (level.getLevel() instanceof ServerLevel serverLevel) {
                BlockPos targetPos = pos.immutable();
                long spawnSeed = random.nextLong();
                MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> configureGunnerSpawner(serverLevel, targetPos, spawnSeed));
            }
        }

        private void configureGunnerSpawner(ServerLevel serverLevel, BlockPos pos, long spawnSeed) {
            if (!serverLevel.hasChunkAt(pos)) {
                return;
            }

            var blockEntity = serverLevel.getBlockEntity(pos);
            SpawnerBlockEntity spawner;
            if (blockEntity instanceof SpawnerBlockEntity existing) {
                spawner = existing;
            } else {
                BlockState state = serverLevel.getBlockState(pos);
                if (!state.is(Blocks.SPAWNER)) {
                    return;
                }
                spawner = new SpawnerBlockEntity(pos, state);
                serverLevel.setBlockEntity(spawner);
            }

            RandomSource spawnerRandom = RandomSource.create(spawnSeed);
            spawner.setEntityId(ModEntities.PHANTOM_GUNNER.get(), spawnerRandom);
            spawner.setChanged();

            RandomSource spawnRandom = RandomSource.create(spawnSeed ^ 0x5deece66dL);
            spawnImmediateGunner(serverLevel, pos, spawnRandom);
        }

        private void scheduleDamagedElytraFrame(ServerLevel serverLevel, BlockPos marker, Direction facing) {
            MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> spawnDamagedElytraFrame(serverLevel, marker, facing));
        }

        private void spawnDamagedElytraFrame(ServerLevel serverLevel, BlockPos marker, Direction facing) {
            if (!serverLevel.hasChunkAt(marker)) {
                return;
            }
            if (!serverLevel.getEntitiesOfClass(ItemFrame.class, new AABB(marker)).isEmpty()) {
                // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Elytra marker {} already occupied by frame", marker);
                return;
            }
            ItemFrame frame = new ItemFrame(serverLevel, marker, facing);
            ItemStack stack = new ItemStack(Items.ELYTRA);
            int maxDamage = stack.getMaxDamage();
            stack.setDamageValue(maxDamage);
            frame.setItem(stack, false);
            serverLevel.addFreshEntity(frame);
            // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Spawned broken Elytra frame at {}", marker);
        }

        private void scheduleGuardianSpawn(ServerLevel serverLevel, BlockPos anchorBase, long spawnSeed) {
            MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> spawnGuardian(serverLevel, anchorBase, spawnSeed));
        }

        private void spawnGuardian(ServerLevel serverLevel, BlockPos anchorBase, long spawnSeed) {
            if (!serverLevel.hasChunkAt(anchorBase)) {
                return;
            }
            if (!serverLevel.getEntitiesOfClass(TerrorPhantomGuardian.class, new AABB(anchorBase).inflate(4.0D)).isEmpty()) {
                return;
            }
            TerrorPhantomGuardian guardian = ModEntities.TERROR_PHANTOM_GUARDIAN.get().create(serverLevel);
            if (guardian == null) {
                return;
            }
            int deckHeight = findDeckHeight(serverLevel, anchorBase);
            BlockPos deckAnchor = new BlockPos(anchorBase.getX(), deckHeight, anchorBase.getZ());
            BlockPos spawnAbove = deckAnchor.above(14);
            RandomSource spawnRandom = RandomSource.create(spawnSeed);
            guardian.setPos(spawnAbove.getX() + 0.5D, spawnAbove.getY(), spawnAbove.getZ() + 0.5D);
            guardian.setYRot(spawnRandom.nextFloat() * 360.0F);
            guardian.setXRot(0.0F);
            guardian.yRotO = guardian.getYRot();
            guardian.xRotO = guardian.getXRot();
            DifficultyInstance difficulty = new DifficultyInstance(serverLevel.getDifficulty(), serverLevel.getDayTime(), 0L, serverLevel.getMoonBrightness());
            guardian.finalizeSpawn(serverLevel, difficulty, MobSpawnType.EVENT, null);
            guardian.initialiseDeckAnchor(deckAnchor, GUARDIAN_TETHER_RADIUS);
            serverLevel.addFreshEntity(guardian);
        }

        private int findDeckHeight(ServerLevel serverLevel, BlockPos base) {
            MutableBlockPos cursor = new MutableBlockPos();
            int minY = serverLevel.dimensionType().minY();
            int top = Math.min(minY + serverLevel.dimensionType().logicalHeight() - 1, base.getY() + 48);
            int bottom = Math.max(minY, base.getY() - 48);
            for (int y = top; y >= bottom; y--) {
                cursor.set(base.getX(), y, base.getZ());
                BlockState state = serverLevel.getBlockState(cursor);
                if (!state.isAir() && !state.getCollisionShape(serverLevel, cursor).isEmpty()) {
                    return y;
                }
            }
            return base.getY();
        }

        private boolean isInteriorSpawnerPosition(ServerLevelAccessor level, BlockPos pos) {
            BlockState below = level.getBlockState(pos.below());
            boolean solidFloor = !below.isAir() && !below.getCollisionShape(level, pos.below()).isEmpty();
            if (!solidFloor) {
                return false;
            }

            int solidWalls = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighbor = level.getBlockState(neighborPos);
                if (!neighbor.isAir() && !neighbor.getCollisionShape(level, neighborPos).isEmpty()) {
                    solidWalls++;
                }
            }

            boolean hasCeiling = false;
            for (int dy = 1; dy <= 2; dy++) {
                BlockPos abovePos = pos.above(dy);
                BlockState aboveState = level.getBlockState(abovePos);
                if (!aboveState.isAir() && !aboveState.getCollisionShape(level, abovePos).isEmpty()) {
                    hasCeiling = true;
                    break;
                }
            }

            return hasCeiling || solidWalls >= 2;
        }

        private void spawnImmediateGunner(ServerLevel level, BlockPos pos, RandomSource random) {
            var gunner = ModEntities.PHANTOM_GUNNER.get().create(level);
            if (gunner == null) {
                return;
            }
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 1.0D;
            double z = pos.getZ() + 0.5D;
            gunner.setPos(x, y, z);
            gunner.setYRot(random.nextFloat() * 360.0F);
            gunner.setPersistenceRequired();
            gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            level.addFreshEntity(gunner);
        }

        private void placeDamagedElytra(ServerLevelAccessor level, RandomSource random) {
            if (this.elytraMarkers.isEmpty()) {
                return;
            }
            ServerLevel serverLevel = level.getLevel();
            if (serverLevel == null) {
                return;
            }
            Direction facing = this.getRotation().rotate(Direction.SOUTH);
            // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Processing {} Elytra markers for ship {} (loot={})", this.elytraMarkers.size(), this.templatePosition, this.hasLootChest);
            for (BlockPos marker : this.elytraMarkers) {
                if (!this.boundingBox.isInside(marker)) {
                    // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Skipping Elytra marker {} outside piece bounds", marker);
                    continue;
                }
                float removalRoll = random.nextFloat();
                boolean remove = removalRoll < 0.6F;
                // JustEnoughGuns.LOGGER.debug("[SkyShipArmada] Elytra marker {} roll={} remove={}", marker, removalRoll, remove);
                if (remove) {
                    continue;
                }
                scheduleDamagedElytraFrame(serverLevel, marker.immutable(), facing);
            }
            this.elytraMarkers.clear();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean(DAMAGE_FLAG, this.damagedElytra);
            tag.putBoolean(LOOT_FLAG, this.hasLootChest);
        }

        private void carveDamagedSections(ServerLevelAccessor level, BoundingBox box, RandomSource random) {
            StructureTemplate template = this.template();
            Vec3i size = template.getSize();
            int length = size.getZ();
            int third = Math.max(1, length / 3);

            int mode = random.nextInt(3);
            int startZ;
            int endZ;
            if (mode == 0) {
                startZ = 0;
                endZ = third;
            } else if (mode == 1) {
                startZ = length - third;
                endZ = length;
            } else {
                startZ = third;
                endZ = length - third;
            }

            for (int x = 0; x < size.getX(); x++) {
                for (int y = 0; y < size.getY(); y++) {
                    for (int z = startZ; z < endZ; z++) {
                        BlockPos relative = new BlockPos(x, y, z);
                        BlockPos worldPos = StructureTemplate.calculateRelativePosition(this.placeSettings, relative).offset(this.templatePosition);
                        if (box.isInside(worldPos)) {
                            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 18);
                        }
                    }
                }
            }
        }
    }

    public static class GuardianSpawnPiece extends StructurePiece {
        private BlockPos spawnPos;

        public GuardianSpawnPiece(BlockPos pos) {
            super(ModStructures.SKY_SHIP_GUARDIAN_PIECE.get(), 0, new BoundingBox(pos));
            this.spawnPos = pos.immutable();
        }

        public GuardianSpawnPiece(CompoundTag tag) {
            super(ModStructures.SKY_SHIP_GUARDIAN_PIECE.get(), tag);
            int x = tag.contains("SpawnX") ? tag.getInt("SpawnX") : 0;
            int y = tag.contains("SpawnY") ? tag.getInt("SpawnY") : 0;
            int z = tag.contains("SpawnZ") ? tag.getInt("SpawnZ") : 0;
            this.spawnPos = new BlockPos(x, y, z);
            this.boundingBox = new BoundingBox(this.spawnPos);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("SpawnX", this.spawnPos.getX());
            tag.putInt("SpawnY", this.spawnPos.getY());
            tag.putInt("SpawnZ", this.spawnPos.getZ());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            ServerLevel serverLevel = level.getLevel();
            BlockPos anchorBase = this.spawnPos.immutable();
            scheduleGuardianSpawn(serverLevel, anchorBase, positionSeed(anchorBase, random.nextLong()));
        }

        private void scheduleGuardianSpawn(ServerLevel serverLevel, BlockPos anchorBase, long spawnSeed) {
            MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> spawnGuardian(serverLevel, anchorBase, spawnSeed));
        }

        private void spawnGuardian(ServerLevel serverLevel, BlockPos anchorBase, long spawnSeed) {
            if (!serverLevel.hasChunkAt(anchorBase)) {
                return;
            }
            if (!serverLevel.getEntitiesOfClass(TerrorPhantomGuardian.class, new AABB(anchorBase).inflate(4.0D)).isEmpty()) {
                return;
            }
            TerrorPhantomGuardian guardian = ModEntities.TERROR_PHANTOM_GUARDIAN.get().create(serverLevel);
            if (guardian == null) {
                return;
            }
            int deckHeight = findDeckHeight(serverLevel, anchorBase);
            BlockPos deckAnchor = new BlockPos(anchorBase.getX(), deckHeight, anchorBase.getZ());
            BlockPos spawnAbove = deckAnchor.above(14);
            RandomSource spawnRandom = RandomSource.create(spawnSeed);
            guardian.setPos(spawnAbove.getX() + 0.5D, spawnAbove.getY(), spawnAbove.getZ() + 0.5D);
            guardian.setYRot(spawnRandom.nextFloat() * 360.0F);
            guardian.setXRot(0.0F);
            guardian.yRotO = guardian.getYRot();
            guardian.xRotO = guardian.getXRot();
            DifficultyInstance difficulty = new DifficultyInstance(serverLevel.getDifficulty(), serverLevel.getDayTime(), 0L, serverLevel.getMoonBrightness());
            guardian.finalizeSpawn(serverLevel, difficulty, MobSpawnType.EVENT, null);
            guardian.initialiseDeckAnchor(deckAnchor, GUARDIAN_TETHER_RADIUS);
            serverLevel.addFreshEntity(guardian);
        }

        private int findDeckHeight(ServerLevel serverLevel, BlockPos base) {
            MutableBlockPos cursor = new MutableBlockPos();
            int minY = serverLevel.dimensionType().minY();
            int top = Math.min(minY + serverLevel.dimensionType().logicalHeight() - 1, base.getY() + 48);
            int bottom = Math.max(minY, base.getY() - 48);
            for (int y = top; y >= bottom; y--) {
                cursor.set(base.getX(), y, base.getZ());
                BlockState state = serverLevel.getBlockState(cursor);
                if (!state.isAir() && !state.getCollisionShape(serverLevel, cursor).isEmpty()) {
                    return y;
                }
            }
            return base.getY();
        }

        private static long positionSeed(BlockPos pos, long extraSeed) {
            return (pos.asLong() ^ 0x9e3779b97f4a7c15L) ^ extraSeed;
        }
    }
}
