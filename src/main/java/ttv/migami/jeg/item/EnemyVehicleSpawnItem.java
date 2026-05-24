package ttv.migami.jeg.item;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public class EnemyVehicleSpawnItem extends Item {
    private static final ResourceLocation RIFLE_AMMO = Reference.id("rifle_ammo");
    private static final ResourceLocation SMALL_SHELL = Reference.id("small_shell");
    private static final ResourceLocation AUTOCANNON_SHELL = Reference.id("autocannon_shell");
    private static final ResourceLocation SMALL_ROCKET = Reference.id("small_rocket");
    private static final ResourceLocation MEDIUM_ANTI_GROUND_MISSILE = Reference.id("medium_anti_ground_missile");

    private final Supplier<EntityType<? extends VehicleEntity>> vehicleType;
    private final ResourceLocation vehicleId;

    public EnemyVehicleSpawnItem(Supplier<EntityType<? extends VehicleEntity>> vehicleType, ResourceLocation vehicleId, Properties properties) {
        super(properties);
        this.vehicleType = vehicleType;
        this.vehicleId = vehicleId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        BlockPos spawnPos = state.getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());
        VehicleEntity vehicle = this.spawnVehicle(serverLevel, spawnPos, context.getPlayer(), context.getItemInHand(), !Objects.equals(clickedPos, spawnPos) && context.getClickedFace() == Direction.UP);
        return vehicle == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos blockPos = hitResult.getBlockPos();
        if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide()) {
            if (!(level instanceof ServerLevel serverLevel) || !level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos, hitResult.getDirection(), stack)) {
                return InteractionResultHolder.fail(stack);
            }
            if (this.spawnVehicle(serverLevel, blockPos, player, stack, false) == null) {
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private VehicleEntity spawnVehicle(ServerLevel level, BlockPos pos, Player player, ItemStack stack, boolean offsetY) {
        VehicleEntity vehicle = this.vehicleType.get().create(level);
        if (vehicle == null) {
            return null;
        }
        double y = offsetY ? pos.getY() + 0.5D : pos.getY();
        vehicle.moveTo(pos.getX() + 0.5D, y, pos.getZ() + 0.5D, player == null ? 0.0F : player.getYRot(), 0.0F);
        vehicle.addTag(EnemyVehicleController.ENEMY_VEHICLE_TAG);
        vehicle.getPersistentData().putString(EnemyVehicleController.VEHICLE_KIND_TAG, this.vehicleId.toString());
        EnemyVehicleController.setAnchor(vehicle, vehicle.position());
        loadDefaultAmmo(vehicle);
        vehicle.reloadAiVehicleWeapons();
        if (!level.addFreshEntity(vehicle)) {
            return null;
        }

        int crewCount = "mi28".equals(this.vehicleId.getPath()) ? 2 : 1;
        for (int seat = 0; seat < crewCount; seat++) {
            Pillager crew = this.createCrew(level, vehicle);
            if (crew == null) {
                vehicle.destroyFromEnemyCrewLoss();
                return null;
            }
            if (seat == 0) {
                vehicle.getPersistentData().putUUID(EnemyVehicleController.CREW_ID_TAG, crew.getUUID());
            }
            level.addFreshEntity(crew);
            vehicle.rememberSeatAssignment(crew, seat);
            crew.startRiding(vehicle, true);
        }

        level.gameEvent(player, GameEvent.ENTITY_PLACE, vehicle.position());
        stack.consume(1, player);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return vehicle;
    }

    private Pillager createCrew(ServerLevel level, VehicleEntity vehicle) {
        Pillager crew = EntityType.PILLAGER.create(level);
        if (crew == null) {
            return null;
        }
        crew.moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), vehicle.getYRot(), 0.0F);
        crew.finalizeSpawn(level, level.getCurrentDifficultyAt(crew.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        crew.addTag(GunEvents.JEG_GUNNER_TAG);
        crew.addTag(EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG);
        GunnerMobSpawner.normalizeGunnerMob(crew);
        EnemyVehicleController.configureCrew(crew);
        return crew;
    }

    private void loadDefaultAmmo(VehicleEntity vehicle) {
        if ("lav150".equals(this.vehicleId.getPath())) {
            vehicle.addAmmoForAi(SMALL_SHELL, 128);
            vehicle.addAmmoForAi(RIFLE_AMMO, 512);
        } else if ("bmp2".equals(this.vehicleId.getPath())) {
            vehicle.addAmmoForAi(AUTOCANNON_SHELL, 160);
            vehicle.addAmmoForAi(RIFLE_AMMO, 512);
        } else if ("ah6".equals(this.vehicleId.getPath())) {
            vehicle.addAmmoForAi(RIFLE_AMMO, 600);
            vehicle.addAmmoForAi(SMALL_ROCKET, 48);
        } else if ("mi28".equals(this.vehicleId.getPath())) {
            vehicle.addAmmoForAi(RIFLE_AMMO, 900);
            vehicle.addAmmoForAi(SMALL_ROCKET, 96);
            vehicle.addAmmoForAi(MEDIUM_ANTI_GROUND_MISSILE, 32);
        }
    }
}
