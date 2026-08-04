package ttv.migami.jeg.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDamageTypes;

/**
 * Ensures Free the End / MOB_KILLS see lastHurtByPlayer when the causing entity is a player.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class PlayerKillCreditEvents {
    private PlayerKillCreditEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        attributeFromSource(event.getEntity(), event.getSource());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        attributeFromSource(event.getEntity(), event.getSource());
    }

    private static void attributeFromSource(LivingEntity target, DamageSource source) {
        if (source == null) {
            return;
        }
        Entity causing = source.getEntity();
        if (causing instanceof Player player) {
            ModDamageTypes.attributePlayerKillCredit(target, player);
        }
    }
}
