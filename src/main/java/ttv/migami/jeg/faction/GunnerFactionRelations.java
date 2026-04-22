package ttv.migami.jeg.faction;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import ttv.migami.jeg.event.GunEvents;

public final class GunnerFactionRelations {
    private static final String LEGACY_PILLAGER_GUNNER_TAG = "jeg_pillager_gunner";

    private GunnerFactionRelations() {}

    public static boolean isTaggedGunner(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.entityTags().contains(GunEvents.JEG_GUNNER_TAG)
                || entity.entityTags().contains(GunEvents.JEG_ELITE_GUNNER_TAG)
                || entity.entityTags().contains(LEGACY_PILLAGER_GUNNER_TAG);
    }

    public static boolean areSameFactionGunners(@Nullable LivingEntity first, @Nullable LivingEntity second) {
        if (first == null || second == null || first == second) {
            return false;
        }
        if (!isTaggedGunner(first) || !isTaggedGunner(second)) {
            return false;
        }

        String firstFaction = resolveFactionName(first);
        String secondFaction = resolveFactionName(second);
        return firstFaction != null && firstFaction.equals(secondFaction);
    }

    @Nullable
    private static String resolveFactionName(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return null;
        }
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (entityId == null) {
            return null;
        }

        Faction faction = GunnerManager.getInstance().getFactionForMob(entityId);
        return faction != null ? faction.getName() : null;
    }
}
