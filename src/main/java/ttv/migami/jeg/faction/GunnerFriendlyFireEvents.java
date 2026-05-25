package ttv.migami.jeg.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class GunnerFriendlyFireEvents {
    private static final String ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher").toString();
    private static final int VEHICLE_STRIKE_AGGRO_IGNORE_TICKS = 40;
    private static final Map<UUID, IgnoredVehicleStrike> IGNORED_VEHICLE_STRIKES = new HashMap<>();

    private GunnerFriendlyFireEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (shouldCancelFriendlyRocketDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
        LivingEntity attacker = friendlyVehicleStrikeAttacker(event.getEntity(), event.getSource());
        if (attacker != null) {
            rememberIgnoredVehicleStrike(event.getEntity(), attacker);
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

    private static void rememberIgnoredVehicleStrike(LivingEntity target, LivingEntity attacker) {
        IGNORED_VEHICLE_STRIKES.put(target.getUUID(), new IgnoredVehicleStrike(attacker.getUUID(), target.level().getGameTime() + VEHICLE_STRIKE_AGGRO_IGNORE_TICKS));
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
        if (target == null || !target.isAlive() || target.getUUID().equals(ignored.attackerId())) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            IGNORED_VEHICLE_STRIKES.remove(mob.getUUID());
        }
    }

    private record IgnoredVehicleStrike(UUID attackerId, long expiresAt) {}
}
