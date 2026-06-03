package ttv.migami.jeg.event;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.block.DynamicLightBlock;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.particle.LaserOption;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.network.NetworkHandler;

public final class AttachmentRuntimeEvents {
    private static final double LASER_BEAM_START_OFFSET = 0.75D;
    private static final double LASER_BEAM_STEP = 0.4D;

    private AttachmentRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || player.isDeadOrDying() || player.isSpectator()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (player.isSprinting() && !player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        boolean refreshFlashlight = false;
        if (stack.getItem() instanceof GunItem) {
            if (GunAttachments.modifiers(stack).laserPointer()) {
                tickLaserPointer(player);
            }
            refreshFlashlight = Config.allowFlashlights() && GunAttachments.tickFlashlightBattery(stack, player);
        }

        refreshFlashlight |= Config.allowFlashlights()
                && (FlashlightAttachmentItem.isPowered(player.getMainHandItem())
                || FlashlightAttachmentItem.isPowered(player.getOffhandItem()));
        if (refreshFlashlight) {
            tickFlashlight(player);
        }
    }

    private static void tickLaserPointer(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(100.0D));
        BlockHitResult blockResult = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        EntityHitResult entityResult = rayTraceEntities(player, start, end, 100.0D);

        Vec3 laserEnd = blockResult.getLocation();
        boolean hitBlock = blockResult.getType() == HitResult.Type.BLOCK;
        if (entityResult != null && start.distanceTo(entityResult.getLocation()) < start.distanceTo(blockResult.getLocation())) {
            laserEnd = entityResult.getLocation();
            hitBlock = false;
            Entity hitEntity = entityResult.getEntity();
            if (hitEntity instanceof LivingEntity livingEntity && Config.glowingLaserPointers() && NetworkHandler.isAiming(player)) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 5, 0, false, false, true));
            }
        }

        for (Class<? extends PathfinderMob> mobType : List.of(Cat.class, Ocelot.class)) {
            List<? extends PathfinderMob> mobs = player.level().getEntitiesOfClass(
                    mobType,
                    new AABB(laserEnd.subtract(5.0D, 5.0D, 5.0D), laserEnd.add(5.0D, 5.0D, 5.0D))
            );
            for (PathfinderMob mob : mobs) {
                if (mob instanceof Cat cat && cat.isInSittingPose()) {
                    continue;
                }
                if (mob.getRandom().nextFloat() < 0.02F) {
                    mob.getJumpControl().jump();
                }
                mob.getNavigation().moveTo(laserEnd.x, laserEnd.y, laserEnd.z, 1.2D);
            }
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        emitLaserPointerBeamParticles(serverLevel, start, laserEnd);
        if (hitBlock) {
            emitLaserPointerBlockParticle(serverLevel, blockResult);
        }
        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                laserEnd.x,
                laserEnd.y,
                laserEnd.z,
                1,
                0.01D,
                0.01D,
                0.01D,
                0.0D
        );
    }

    private static void emitLaserPointerBeamParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 offset = end.subtract(start);
        double length = offset.length();
        if (length <= LASER_BEAM_START_OFFSET) {
            return;
        }

        Vec3 normal = offset.normalize();
        for (double distance = LASER_BEAM_START_OFFSET; distance < length; distance += LASER_BEAM_STEP) {
            Vec3 point = start.add(normal.scale(distance));
            level.sendParticles(
                    ModParticleTypes.ENTITY_LASER.get(),
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static void emitLaserPointerBlockParticle(ServerLevel level, BlockHitResult result) {
        Direction face = result.getDirection();
        Vec3 hit = result.getLocation();
        level.sendParticles(
                new LaserOption(face, result.getBlockPos()),
                hit.x + 0.005D * face.getStepX(),
                hit.y + 0.005D * face.getStepY(),
                hit.z + 0.005D * face.getStepZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static void tickFlashlight(Player player) {
        if (!Config.allowFlashlights()) {
            return;
        }

        Level level = player.level();
        double distance = 2.0D;
        for (int index = 0; index < Config.flashlightDistance(); index++) {
            BlockHitResult result = level.clip(new ClipContext(
                    player.getEyePosition(1.0F),
                    player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(distance)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));
            refreshDynamicLight(level, result.getBlockPos());
            distance += 1.0D;
        }
    }

    private static void refreshDynamicLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.DYNAMIC_LIGHT.get())) {
            DynamicLightBlock.setDelay(level, pos, 2.0D);
        } else if (state.isAir()) {
            level.setBlock(pos, ModBlocks.DYNAMIC_LIGHT.get().defaultBlockState(), 3);
        } else if (state.is(Blocks.WATER)) {
            BlockState dynamicLight = ModBlocks.DYNAMIC_LIGHT.get()
                    .defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true);
            level.setBlock(pos, dynamicLight, 3);
        }
    }

    private static EntityHitResult rayTraceEntities(Player player, Vec3 start, Vec3 end, double range) {
        Level level = player.level();
        EntityHitResult closest = null;
        double closestDistance = range;

        for (Entity entity : level.getEntities(player, new AABB(start, start).inflate(range), Entity::isPickable)) {
            AABB box = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> hitPos = box.clip(start, end);
            if (hitPos.isEmpty()) {
                continue;
            }
            double distance = start.distanceTo(hitPos.get());
            if (distance < closestDistance) {
                closest = new EntityHitResult(entity, hitPos.get());
                closestDistance = distance;
            }
        }

        return closest;
    }
}
