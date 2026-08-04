package ttv.migami.jeg.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
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
        return new DamageSource(registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(VEHICLE_STRIKE), directEntity, attacker);
    }

    public static DamageSource causeRepairToolDamage(RegistryAccess registryAccess, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(REPAIR_TOOL), attacker, attacker);
    }

    public static DamageSource causeBulletDamage(RegistryAccess registryAccess, @Nullable Entity directEntity, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BULLET), directEntity, attacker);
    }

    /**
     * Marks a player as responsible for upcoming damage. Must run before hurt/die for Free the End / MOB_KILLS.
     * Creative included; spectators skipped.
     */
    public static void attributePlayerKillCredit(LivingEntity target, @Nullable Entity attacker) {
        if (!(attacker instanceof Player player) || player.isSpectator()) {
            return;
        }
        target.setLastHurtByMob(player);
        target.setLastHurtByPlayer(player);
    }

    public static boolean hurtWithPlayerKillCredit(
            LivingEntity target,
            DamageSource source,
            float damage,
            @Nullable Entity attacker
    ) {
        attributePlayerKillCredit(target, attacker);
        return target.hurt(source, damage);
    }

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

    public static @Nullable LivingEntity resolveLivingTarget(@Nullable Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof EnderDragonPart part) {
            return part.parentMob;
        }
        return null;
    }
}
