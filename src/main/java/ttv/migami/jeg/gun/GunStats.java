package ttv.migami.jeg.gun;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

public record GunStats(
        ResourceLocation id,
        @Nullable ResourceLocation ammoItem,
        String reloadType,
        int magazineSize,
        int reloadTime,
        int additionalReloadTime,
        int fireDelay,
        float damage,
        float projectileSpeed,
        int projectileLife,
        boolean gravity,
        float spread,
        float headshotMultiplier,
        int projectileAmount,
        @Nullable ResourceLocation fireSound,
        @Nullable ResourceLocation silencedFireSound,
        @Nullable ResourceLocation enchantedFireSound,
        @Nullable ResourceLocation reloadStartSound,
        @Nullable ResourceLocation reloadLoadSound,
        @Nullable ResourceLocation reloadEndSound,
        @Nullable ResourceLocation ejectorPullSound,
        @Nullable ResourceLocation ejectorReleaseSound,
        float projectileSize,
        int trailColor,
        float trailLengthMultiplier
) {
    public boolean usesMagazine() {
        return switch (reloadType) {
            case "jeg:mag_fed", "jeg:magazine" -> true;
            default -> false;
        };
    }

    public boolean isInventoryFed() {
        return "jeg:inventory_fed".equals(reloadType);
    }

    public int totalReloadTime() {
        return Math.max(0, reloadTime + additionalReloadTime);
    }

    public Optional<SoundEvent> fireSoundEvent() {
        return resolveSound(fireSound);
    }

    public Optional<SoundEvent> silencedFireSoundEvent() {
        return resolveSound(silencedFireSound);
    }

    public Optional<SoundEvent> enchantedFireSoundEvent() {
        return resolveSound(enchantedFireSound);
    }

    public Optional<SoundEvent> reloadStartSoundEvent() {
        return resolveSound(reloadStartSound);
    }

    public Optional<SoundEvent> reloadLoadSoundEvent() {
        return resolveSound(reloadLoadSound);
    }

    public Optional<SoundEvent> reloadEndSoundEvent() {
        return resolveSound(reloadEndSound);
    }

    private Optional<SoundEvent> resolveSound(@Nullable ResourceLocation location) {
        if (location == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.SOUND_EVENT.getOptional(location);
    }

    public float clampedProjectileSize() {
        return Math.max(0.01F, projectileSize);
    }

    public float clampedTrailLength() {
        return Math.max(0.1F, trailLengthMultiplier);
    }

    public float recoilKick() {
        float base = Math.max(0.05F, damage / 18.0F);
        if (projectileAmount > 1) {
            base += (projectileAmount - 1) * 0.02F;
        }
        base += Mth.clamp(spread / 45.0F, 0.0F, 0.15F);
        return Math.min(0.6F, base);
    }
}
