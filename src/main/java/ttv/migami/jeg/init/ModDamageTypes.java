package ttv.migami.jeg.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> VEHICLE_STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, Reference.id("vehicle_strike"));
    public static final ResourceKey<DamageType> REPAIR_TOOL = ResourceKey.create(Registries.DAMAGE_TYPE, Reference.id("repair_tool"));
    /** Player/gunner bullet hits; causing entity must be the shooter for kill credit. */
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Reference.id("bullet"));

    private ModDamageTypes() {
    }

    public static DamageSource causeVehicleStrikeDamage(RegistryAccess registryAccess, @Nullable Entity directEntity, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(VEHICLE_STRIKE), directEntity, attacker);
    }

    public static DamageSource causeRepairToolDamage(RegistryAccess registryAccess, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(REPAIR_TOOL), attacker, attacker);
    }

    public static DamageSource causeBulletDamage(RegistryAccess registryAccess, @Nullable Entity directEntity, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BULLET), directEntity, attacker);
    }

    /**
     * Marks a player as responsible for upcoming damage.
     * <p>
     * Must be called <b>before</b> {@link LivingEntity#hurtServer} / any blast that may kill the
     * target. In 26.x, {@code die()} awards {@code MOB_KILLS} and Free the End from
     * {@link LivingEntity#getKillCredit()}, which only sees {@code lastHurtByPlayer} already set
     * at death time.
     * <p>
     * Creative is included: vanilla still awards Free the End / kill stats in creative. Spectators
     * are skipped.
     */
    public static void attributePlayerKillCredit(LivingEntity target, @Nullable Entity attacker) {
        if (!(attacker instanceof Player player) || player.isSpectator()) {
            return;
        }
        target.setLastHurtByMob(player);
        // 26.x requires memory time ticks (vanilla hurt path used 100 when it still resolved this).
        target.setLastHurtByPlayer(player, 100);
    }

    /**
     * Attribute kill credit then apply server damage so one-shot kills still award stats.
     */
    public static boolean hurtWithPlayerKillCredit(
            LivingEntity target,
            ServerLevel level,
            DamageSource source,
            float damage,
            @Nullable Entity attacker
    ) {
        attributePlayerKillCredit(target, attacker);
        return target.hurtServer(level, source, damage);
    }

    /**
     * Pre-attributes nearby living targets so vanilla {@link ServerLevel#explode} entity damage
     * still counts as player kills when the explosion source itself is not a player/projectile.
     */
    public static void attributePlayerKillCreditInRadius(
            ServerLevel level,
            Vec3 center,
            double radius,
            @Nullable Entity attacker
    ) {
        if (!(attacker instanceof Player player) || player.isSpectator()) {
            return;
        }
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target != player) {
                attributePlayerKillCredit(target, player);
            }
        }
    }

    /**
     * Resolve multipart hitboxes (e.g. {@code EnderDragonPart}) to the parent living entity for
     * kill credit. Returns the entity itself when it is already living.
     */
    public static @Nullable LivingEntity resolveLivingTarget(@Nullable Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part) {
            return part.parentMob;
        }
        if (entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
