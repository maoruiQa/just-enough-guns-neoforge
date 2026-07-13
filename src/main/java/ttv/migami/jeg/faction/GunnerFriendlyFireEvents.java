package ttv.migami.jeg.faction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import ttv.migami.jeg.fabric.compat.neoforge.bus.api.SubscribeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.fml.common.EventBusSubscriber;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class GunnerFriendlyFireEvents {
    private static final String ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher").toString();
    private static final int VEHICLE_STRIKE_AGGRO_IGNORE_TICKS = 200;
    private static final Map<UUID, IgnoredVehicleStrike> IGNORED_VEHICLE_STRIKES = new HashMap<>();

    private GunnerFriendlyFireEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (shouldCancelFriendlyRocketDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
        LivingEntity attacker = friendlyVehicleStrikeAttacker(event.getEntity(), event.getSource());
        if (attacker != null) {
            rememberIgnoredVehicleStrike(event.getEntity(), event.getSource(), attacker);
        }
    }

    public static boolean shouldCancelFriendlyRocketDamage(LivingEntity target, @Nullable DamageSource source) {
        if (source == null || !(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return false;
        }
        if (!ROCKET_LAUNCHER_ID.equals(bullet.getGunId())) {
            return false;
        }

        Entity attacker = source.getEntity();
        LivingEntity livingAttacker = attacker instanceof LivingEntity living ? living : null;
        if (livingAttacker == null && bullet.getOwner() instanceof LivingEntity owner) {
            livingAttacker = owner;
        }
        return livingAttacker != null && GunnerFactionRelations.areSameFactionGunners(livingAttacker, target);
    }

    @Nullable
    private static LivingEntity friendlyVehicleStrikeAttacker(LivingEntity target, @Nullable DamageSource source) {
        if (source == null || !source.is(ModDamageTypes.VEHICLE_STRIKE)) {
            return null;
        }
        LivingEntity attacker = resolveVehicleStrikeGunner(source);
        return attacker != null && GunnerFactionRelations.areSameFactionGunners(attacker, target) ? attacker : null;
    }

    @Nullable
    private static LivingEntity resolveVehicleStrikeGunner(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        if (source.getDirectEntity() instanceof VehicleEntity vehicle) {
            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof LivingEntity living && GunnerFactionRelations.isTaggedGunner(living)) {
                    return living;
                }
            }
        }
        return null;
    }

    public static void clearFriendlyVehicleStrikeTargetAfterDamage(LivingEntity target, DamageSource source) {
        LivingEntity attacker = friendlyVehicleStrikeAttacker(target, source);
        if (attacker == null) {
            return;
        }
        rememberIgnoredVehicleStrike(target, source, attacker);
        if (target instanceof Mob mob) {
            clearIgnoredVehicleStrikeTarget(mob);
        }
    }

    private static void rememberIgnoredVehicleStrike(LivingEntity target, DamageSource source, LivingEntity attacker) {
        Set<UUID> attackerIds = new HashSet<>();
        attackerIds.add(attacker.getUUID());
        if (source.getDirectEntity() instanceof VehicleEntity vehicle) {
            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof LivingEntity living && GunnerFactionRelations.isTaggedGunner(living)) {
                    attackerIds.add(living.getUUID());
                }
            }
        }
        IGNORED_VEHICLE_STRIKES.put(target.getUUID(), new IgnoredVehicleStrike(Set.copyOf(attackerIds), target.level().getGameTime() + VEHICLE_STRIKE_AGGRO_IGNORE_TICKS));
    }

    public static void clearIgnoredVehicleStrikeTarget(Mob mob) {
        if (!GunnerFactionRelations.isTaggedGunner(mob)) {
            return;
        }
        IgnoredVehicleStrike ignored = IGNORED_VEHICLE_STRIKES.get(mob.getUUID());
        if (ignored == null) {
            return;
        }
        if (mob.level().getGameTime() > ignored.expiresAt()) {
            IGNORED_VEHICLE_STRIKES.remove(mob.getUUID());
            return;
        }
        LivingEntity target = mob.getTarget();
        LivingEntity lastHurtByMob = mob.getLastHurtByMob();
        boolean clearTarget = target == null || shouldClearIgnoredVehicleStrikeEntity(target, ignored);
        boolean clearLastHurtByMob = target == null || shouldClearIgnoredVehicleStrikeEntity(lastHurtByMob, ignored);
        if (clearTarget) {
            mob.setTarget(null);
        }
        if (clearLastHurtByMob) {
            mob.setLastHurtByMob(null);
        }
        if (clearTarget || clearLastHurtByMob) {
            mob.setAggressive(false);
        }
    }

    private static boolean shouldClearIgnoredVehicleStrikeEntity(@Nullable LivingEntity entity, IgnoredVehicleStrike ignored) {
        return entity != null && (!entity.isAlive() || entity.isRemoved() || ignored.attackerIds().contains(entity.getUUID()));
    }

    private record IgnoredVehicleStrike(Set<UUID> attackerIds, long expiresAt) {}
}
