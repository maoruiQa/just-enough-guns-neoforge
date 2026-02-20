package ttv.migami.jeg.gun;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;

public final class RecoilProfiles {
    private static final Map<ResourceLocation, Float> MULTIPLIERS = Map.ofEntries(
        Map.entry(Reference.id("waterpipe_shotgun"), 1.6F),
        Map.entry(Reference.id("pump_shotgun"), 1.4F),
        Map.entry(Reference.id("double_barrel_shotgun"), 1.7F),
        Map.entry(Reference.id("repeating_shotgun"), 1.5F),
        Map.entry(Reference.id("supersonic_shotgun"), 1.6F),
        Map.entry(Reference.id("holy_shotgun"), 1.8F),
        Map.entry(Reference.id("grenade_launcher"), 2.1F),
        Map.entry(Reference.id("rocket_launcher"), 2.3F),
        Map.entry(Reference.id("hypersonic_cannon"), 2.5F),
        Map.entry(Reference.id("typhoonee"), 2.0F),
        Map.entry(Reference.id("minigun"), 1.20F),
        Map.entry(Reference.id("light_machine_gun"), 0.70F),
        Map.entry(Reference.id("flamethrower"), 0.0F)
    );

    private RecoilProfiles() {}

    public static float multiplier(ResourceLocation id) {
        return MULTIPLIERS.getOrDefault(id, 1.0F);
    }
}
