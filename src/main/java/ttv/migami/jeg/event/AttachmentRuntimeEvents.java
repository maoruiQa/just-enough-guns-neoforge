package ttv.migami.jeg.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.particle.LaserOption;

public final class AttachmentRuntimeEvents {
    private static final long BAYONET_CHARGE_TICKS = 40L;
    private static final double BAYONET_CHARGE_RANGE = 1.5D;
    private static final double BAYONET_CHARGE_SWEEP_ANGLE = Math.toRadians(100.0D);
    private static final float BAYONET_DAMAGE_DIVISOR = 1.5F;
    private static final int BAYONET_CHARGE_DAMAGE = 15;
    private static final double BAYONET_MELEE_RANGE = 2.0D;
    private static final double BAYONET_MELEE_SWEEP_ANGLE = Math.toRadians(100.0D);
    private static final int BAYONET_MELEE_DAMAGE = 8;
    private static final int BAYONET_MELEE_COOLDOWN_TICKS = 15;
    private static final int BAYONET_SPRINT_MELEE_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> BAYONET_SPRINT_START_TICKS = new HashMap<>();

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
        boolean refreshFlashlight = false;
        if (stack.getItem() instanceof GunItem) {
            tickBayonetCharge(player, stack);
        } else {
            BAYONET_SPRINT_START_TICKS.remove(player.getUUID());
        }

        if (stack.getItem() instanceof GunItem) {
            boolean pauseGunLights = shouldPauseGunPoweredAttachments(player, stack);
            if (!pauseGunLights && GunAttachments.modifiers(stack).laserPointer()) {
                tickLaserPointer(player);
            }
            refreshFlashlight = !pauseGunLights && Config.allowFlashlights() && GunAttachments.tickFlashlightBattery(stack, player);
        }

        refreshFlashlight |= Config.allowFlashlights()
                && (FlashlightAttachmentItem.isPowered(player.getMainHandItem())
                || FlashlightAttachmentItem.isPowered(player.getOffhandItem()));
        if (refreshFlashlight) {
            tickFlashlight(player);
        }
    }

    private static boolean shouldPauseGunPoweredAttachments(Player player, ItemStack gunStack) {
        return player.isSprinting()
                && gunStack.getItem() instanceof GunItem
                && !player.getCooldowns().isOnCooldown(gunStack);
    }

    public static void handleBayonetMelee(ServerPlayer player) {
        ItemStack gunStack = player.getMainHandItem();
        if (!(gunStack.getItem() instanceof GunItem)) {
            return;
        }

        Optional<ItemStack> attachment = GunAttachments.stack(gunStack, AttachmentType.BARREL);
        ItemStack bayonet = attachment.filter(AttachmentRuntimeEvents::isBayonet).orElse(ItemStack.EMPTY);
        Level level = player.level();
        if (player.isSprinting()) {
            Vec3 look = player.getLookAngle();
            player.push(look.x, look.y, look.z);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 1.0F);
        player.getCooldowns().addCooldown(
                gunStack,
                player.isSprinting() ? BAYONET_SPRINT_MELEE_COOLDOWN_TICKS : BAYONET_MELEE_COOLDOWN_TICKS
        );

        boolean hasBayonet = !bayonet.isEmpty();
        float damage = hasBayonet ? bayonetDamage(player, bayonet) / BAYONET_DAMAGE_DIVISOR : 1.0F;
        if (hasBayonet && isBayonetTooDamaged(bayonet)) {
            damage = 0.0F;
        }

        boolean damaged = false;
        int knockback = hasBayonet ? enchantmentLevel(player, bayonet, Enchantments.KNOCKBACK) : 0;
        int fireAspect = hasBayonet ? enchantmentLevel(player, bayonet, Enchantments.FIRE_ASPECT) : 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BAYONET_MELEE_RANGE))) {
            if (target == player || !target.isAlive() || !isInMeleeArc(player, target)) {
                continue;
            }

            Vec3 direction = target.position().subtract(player.position()).normalize();
            target.push(direction.x * knockback, 0.5D, direction.z * knockback);
            if (damage > 0.0F) {
                target.hurt(player.damageSources().playerAttack(player), damage);
                damaged = true;
                if (fireAspect > 0) {
                    target.igniteForSeconds(2.0F * fireAspect);
                }
            }
        }

        if (hasBayonet && damage <= 0.0F) {
            level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 3.0F, 1.0F);
        } else if (hasBayonet && damaged) {
            damageStoredBayonet(player, gunStack, bayonet, BAYONET_MELEE_DAMAGE);
        }

        Vec3 look = player.getLookAngle();
        Vec3 particlePos = player.position().add(look.x * 1.8D, look.y * 1.8D + player.getEyeHeight(), look.z * 1.8D);
        ((ServerLevel) level).sendParticles(ParticleTypes.SWEEP_ATTACK, particlePos.x, particlePos.y, particlePos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void tickBayonetCharge(Player player, ItemStack gunStack) {
        UUID playerId = player.getUUID();
        Optional<ItemStack> attachment = GunAttachments.stack(gunStack, AttachmentType.BARREL);
        if (!player.isSprinting() || attachment.isEmpty() || !isBayonet(attachment.get())) {
            BAYONET_SPRINT_START_TICKS.remove(playerId);
            return;
        }

        long gameTime = player.level().getGameTime();
        BAYONET_SPRINT_START_TICKS.putIfAbsent(playerId, gameTime);
        if (gameTime - BAYONET_SPRINT_START_TICKS.get(playerId) < BAYONET_CHARGE_TICKS) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(gunStack) || player.hasEffect(MobEffects.SLOWNESS)) {
            return;
        }

        ItemStack bayonet = attachment.get();
        float damage = bayonetDamage(player, bayonet) / BAYONET_DAMAGE_DIVISOR;
        if (isBayonetTooDamaged(bayonet)) {
            damage = 0.0F;
        }
        int knockback = enchantmentLevel(player, bayonet, Enchantments.KNOCKBACK);
        int fireAspect = enchantmentLevel(player, bayonet, Enchantments.FIRE_ASPECT);
        int sweepingEdge = enchantmentLevel(player, bayonet, Enchantments.SWEEPING_EDGE);

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 0, false, false));
        if (damage <= 0.0F) {
            handleBayonetBlockCollision(player, knockback);
            return;
        }

        AABB hitBox = player.getBoundingBox().inflate(BAYONET_CHARGE_RANGE);
        boolean damaged = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> target != player && target.isAlive())) {
            if (target.invulnerableTime != 0 || !isInBayonetArc(player, target)) {
                continue;
            }
            player.invulnerableTime = 40;
            if (sweepingEdge < 2) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
            }
            Vec3 direction = target.position().subtract(player.position()).normalize();
            target.push(direction.x * knockback, 0.5D, direction.z * knockback);
            if (damage > 0.0F) {
                target.hurt(player.damageSources().playerAttack(player), damage);
                damaged = true;
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 1.0F);
                if (fireAspect > 0) {
                    target.igniteForSeconds(2.0F * fireAspect);
                }
                ((ServerLevel) player.level()).sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        (int) damage,
                        0.3D,
                        target.getBbHeight(),
                        0.3D,
                        0.2D
                );
            }
            if (sweepingEdge < 3) {
                Vec3 pushBack = player.position().subtract(target.position()).normalize();
                player.push(pushBack.x, 0.5D, pushBack.z);
            }
        }
        handleBayonetBlockCollision(player, knockback);

        if (damaged) {
            damageStoredBayonet(player, gunStack, bayonet, BAYONET_CHARGE_DAMAGE);
        }
    }

    private static void handleBayonetBlockCollision(Player player, int knockback) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(0.5D));
        HitResult hitResult = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        Vec3 pushBack = player.getLookAngle().normalize().scale(-1.0D);
        double pushBackForce = 1.0D + knockback / 4.0D;
        player.level().playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 3.0F, 1.0F);
        player.push(pushBack.x * pushBackForce, 0.5D, pushBack.z * pushBackForce);
    }

    private static boolean isInBayonetArc(Player player, LivingEntity target) {
        Vec3 offset = targetCenter(target).subtract(player.getEyePosition());
        if (offset.lengthSqr() <= 0.0001D) {
            return true;
        }
        return Math.acos(offset.normalize().dot(player.getLookAngle().normalize())) < BAYONET_CHARGE_SWEEP_ANGLE / 3.0D;
    }

    private static boolean isInMeleeArc(Player player, LivingEntity target) {
        Vec3 offset = targetCenter(target).subtract(player.getEyePosition());
        if (offset.lengthSqr() <= 0.0001D) {
            return true;
        }
        return Math.acos(offset.normalize().dot(player.getLookAngle().normalize())) < BAYONET_MELEE_SWEEP_ANGLE / 2.0D;
    }

    private static Vec3 targetCenter(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static float bayonetDamage(Player player, ItemStack bayonet) {
        double baseDamage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        if (isBayonet(bayonet)) {
            baseDamage = 6.0D;
        }
        return (float) (baseDamage + enchantmentLevel(player, bayonet, Enchantments.SHARPNESS));
    }

    private static boolean isBayonet(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().endsWith("_sword");
    }

    private static boolean isBayonetTooDamaged(ItemStack bayonet) {
        return bayonet.isDamageableItem() && bayonet.getDamageValue() > bayonet.getMaxDamage() / BAYONET_DAMAGE_DIVISOR;
    }

    private static void damageStoredBayonet(Player player, ItemStack gunStack, ItemStack bayonet, int amount) {
        if (player.getAbilities().instabuild || !Config.gunDurabilityEnabled() || !bayonet.isDamageableItem()
                || enchantmentLevel(player, bayonet, Enchantments.MENDING) > 0) {
            return;
        }
        int damage = bayonet.getDamageValue() + amount;
        if (damage >= bayonet.getMaxDamage()) {
            player.level().playSound(player, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            GunAttachments.clear(gunStack, AttachmentType.BARREL);
        } else {
            bayonet.setDamageValue(damage);
            GunAttachments.updateStoredStack(gunStack, AttachmentType.BARREL, bayonet);
        }
    }

    private static int enchantmentLevel(Player player, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantment) {
        HolderLookup.RegistryLookup<Enchantment> lookup = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> holder = lookup.get(enchantment);
        return holder.map(value -> EnchantmentHelper.getItemEnchantmentLevel(value, stack)).orElse(0);
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

        if (hitBlock) {
            emitLaserPointerBlockParticle((ServerLevel) player.level(), blockResult);
        }
    }

    private static void emitLaserPointerBlockParticle(ServerLevel level, BlockHitResult result) {
        Direction face = result.getDirection();
        Vec3 hit = result.getLocation();
        level.sendParticles(
                new LaserOption(face, result.getBlockPos()),
                hit.x + 0.03D * face.getStepX(),
                hit.y + 0.03D * face.getStepY(),
                hit.z + 0.03D * face.getStepZ(),
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
