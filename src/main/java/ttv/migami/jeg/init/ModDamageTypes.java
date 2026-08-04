package ttv.migami.jeg.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
     * Ensures vanilla achievements, boss progress, loot {@code killed_by_player}, and modpack
     * kill tracking attribute the kill to a survival/adventure player shooter.
     */
    public static void attributePlayerKillCredit(LivingEntity target, @Nullable Entity attacker) {
        if (!(attacker instanceof Player player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        target.setLastHurtByMob(player);
        target.setLastHurtByPlayer(player);
    }
}
