package ttv.migami.jeg.vehicle.util;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleSoundHelper {
    private static final ResourceLocation LOCK_WARNING = Reference.id("vehicle.lock_warning");
    private static final Map<String, ResourceLocation> FIRE_SOUNDS = Map.ofEntries(
            Map.entry("a10", Reference.id("vehicle.a10.fire")),
            Map.entry("ah6", Reference.id("vehicle.ah6.cannon_fire")),
            Map.entry("bmp2", Reference.id("vehicle.bmp2.cannon_fire")),
            Map.entry("hpj11", Reference.id("vehicle.hpj11.fire")),
            Map.entry("laser_tower", Reference.id("vehicle.laser_tower.fire")),
            Map.entry("lav150", Reference.id("vehicle.lav150.cannon_fire")),
            Map.entry("mi28", Reference.id("vehicle.mi28.cannon_fire")),
            Map.entry("waveforce_tower", Reference.id("vehicle.waveforce_tower.fire"))
    );
    private static final Map<String, ResourceLocation> GUIDED_FIRE_SOUNDS = Map.of(
            "bmp2", Reference.id("vehicle.bmp2.missile_fire")
    );

    private VehicleSoundHelper() {}

    public static SoundEvent lockWarning() {
        DeferredHolder<SoundEvent, SoundEvent> sound = ModSounds.ALL.get(LOCK_WARNING);
        return sound != null ? sound.get() : SoundEvents.NOTE_BLOCK_PLING.value();
    }

    public static SoundEvent fireSound(VehicleEntity vehicle, VehicleWeaponInfo weapon, GunStats fallbackStats) {
        String vehiclePath = vehicle.vehicleDataId().getPath();
        ResourceLocation id = weapon.guided() ? GUIDED_FIRE_SOUNDS.get(vehiclePath) : FIRE_SOUNDS.get(vehiclePath);
        if (id == null) {
            id = FIRE_SOUNDS.get(vehiclePath);
        }
        DeferredHolder<SoundEvent, SoundEvent> sound = id == null ? null : ModSounds.ALL.get(id);
        if (sound != null) {
            return sound.get();
        }
        return fallbackStats.fireSoundEvent().orElse(SoundEvents.CROSSBOW_SHOOT);
    }
}
