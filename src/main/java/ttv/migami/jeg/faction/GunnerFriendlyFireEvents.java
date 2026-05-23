package ttv.migami.jeg.faction;

import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class GunnerFriendlyFireEvents {
    private static final String ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher").toString();

    private GunnerFriendlyFireEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (shouldCancelFriendlyRocketDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
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
}
