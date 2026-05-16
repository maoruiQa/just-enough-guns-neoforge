package ttv.migami.jeg.init;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> VEHICLE_STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, Reference.id("vehicle_strike"));
    public static final ResourceKey<DamageType> REPAIR_TOOL = ResourceKey.create(Registries.DAMAGE_TYPE, Reference.id("repair_tool"));

    private ModDamageTypes() {
    }

    public static DamageSource causeVehicleStrikeDamage(RegistryAccess registryAccess, @Nullable Entity directEntity, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(VEHICLE_STRIKE), directEntity, attacker);
    }

    public static DamageSource causeRepairToolDamage(RegistryAccess registryAccess, @Nullable Entity attacker) {
        return new DamageSource(registryAccess.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(REPAIR_TOOL), attacker, attacker);
    }
}
