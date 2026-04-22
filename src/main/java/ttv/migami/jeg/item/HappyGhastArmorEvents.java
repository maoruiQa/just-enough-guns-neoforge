package ttv.migami.jeg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class HappyGhastArmorEvents {
    private HappyGhastArmorEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof HappyGhast ghast) || ghast.level().isClientSide()) {
            return;
        }

        var harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        if (!HappyGhastArmorHelper.isArmoredHarness(harness)) {
            return;
        }

        float plating = HappyGhastArmorHelper.getPlating(harness);
        if (plating <= 0.0F) {
            return;
        }

        float damage = event.getAmount();
        if (damage <= 0.0F) {
            return;
        }

        JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Incoming damage={} platingBefore={} ghast={}", damage, plating, ghast.getUUID());
        float absorbed = Math.min(plating, damage);
        float remaining = damage - absorbed;
        HappyGhastArmorHelper.removePlating(harness, absorbed);
        HappyGhastArmorHelper.syncAbsorption(ghast);
        notifyPassengers(ghast);

        if (remaining <= 0.0F) {
            JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Damage fully absorbed. Cancelling hit on {}", ghast.getUUID());
            event.setCanceled(true);
        } else {
            JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Damage partially absorbed. Remaining={} on {}", remaining, ghast.getUUID());
            event.setAmount(remaining);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof HappyGhast ghast)) {
            return;
        }

        if (event.getSlot() == EquipmentSlot.BODY) {
            if (HappyGhastArmorHelper.isArmoredHarness(event.getTo())) {
                HappyGhastArmorHelper.syncAbsorption(ghast);
                JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Harness equipped. Plating={} ghast={}", HappyGhastArmorHelper.getPlating(event.getTo()), ghast.getUUID());
                notifyPassengers(ghast);
            } else if (!ghast.level().isClientSide()) {
                ghast.setAbsorptionAmount(0.0F);
                JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Harness removed. Clearing absorption for {}", ghast.getUUID());
                notifyPassengers(ghast);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof HappyGhast ghast) || ghast.level().isClientSide()) {
            return;
        }

        if (HappyGhastArmorHelper.hasHarnessEquipped(ghast)) {
            HappyGhastArmorHelper.syncAbsorption(ghast);
            if (ghast.tickCount % 40 == 0) {
                notifyPassengers(ghast);
            }
        } else if (ghast.getAbsorptionAmount() > 0.0F) {
            ghast.setAbsorptionAmount(0.0F);
        }
    }

    static void notifyPassengers(HappyGhast ghast) {
        if (ghast.level().isClientSide()) {
            return;
        }
        var harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        float plating = HappyGhastArmorHelper.isArmoredHarness(harness)
                ? HappyGhastArmorHelper.getPlating(harness)
                : 0.0F;
        float max = HappyGhastArmorHelper.getMaxPlating(harness);
        for (var passenger : ghast.getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                JustEnoughGuns.LOGGER.debug("[HappyGhast-1.21.9] Notifying passenger {} plating={}", player.getUUID(), plating);
                player.sendSystemMessage(Component.translatable(
                        "tooltip.jeg.harness_status",
                        Math.round(plating),
                        Math.round(max)
                ).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
