package ttv.migami.jeg.vehicle.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModSounds;

public final class VehicleSoundHelper {
    private static final ResourceLocation LOCK_WARNING = Reference.id("vehicle.lock_warning");

    private VehicleSoundHelper() {}

    public static SoundEvent lockWarning() {
        DeferredHolder<SoundEvent, SoundEvent> sound = ModSounds.ALL.get(LOCK_WARNING);
        return sound != null ? sound.get() : SoundEvents.NOTE_BLOCK_PLING.value();
    }
}
