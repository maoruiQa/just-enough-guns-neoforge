package ttv.migami.jeg.vehicle.event;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class VehiclePassengerDamageEvents {
    private VehiclePassengerDamageEvents() {}

    @SubscribeEvent
    public static void onPassengerDamage(LivingIncomingDamageEvent event) {
        Entity passenger = event.getEntity();
        if (!(passenger.getVehicle() instanceof VehicleEntity vehicle) || !vehicle.shouldHidePassenger(passenger)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        if (!shouldBypassVehicle(source)) {
            vehicle.hurt(source, event.getAmount());
            event.setCanceled(true);
        }
    }

    private static boolean shouldBypassVehicle(DamageSource source) {
        return source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.OUTSIDE_BORDER);
    }
}
