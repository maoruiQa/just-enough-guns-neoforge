package ttv.migami.jeg.gun;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;

public final class RecoilProfiles {
    public record Parameters(float angle, float kick, float durationOffset, float adsReduction) {}

    private static final Parameters NONE = new Parameters(0.0F, 0.0F, 0.0F, 0.2F);

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

    private static final Map<ResourceLocation, Parameters> PARAMETERS = Map.ofEntries(
        Map.entry(Reference.id("abstract_gun"), new Parameters(2.7F, 0.22F, 0.0F, 0.2F)),
        Map.entry(Reference.id("assault_rifle"), new Parameters(2.7F, 0.22F, 0.0F, 0.2F)),
        Map.entry(Reference.id("atlantean_spear"), new Parameters(4.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("blossom_rifle"), new Parameters(2.0F, 0.45F, 0.0F, 0.2F)),
        Map.entry(Reference.id("bolt_action_rifle"), new Parameters(4.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("burst_rifle"), new Parameters(2.0F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("combat_pistol"), new Parameters(2.0F, 0.33F, 0.0F, 0.2F)),
        Map.entry(Reference.id("combat_rifle"), new Parameters(4.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("compound_bow"), new Parameters(2.0F, 0.1F, 0.0F, 0.2F)),
        Map.entry(Reference.id("custom_smg"), new Parameters(1.0F, 0.33F, 0.0F, 0.2F)),
        Map.entry(Reference.id("double_barrel_shotgun"), new Parameters(10.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("flamethrower"), new Parameters(0.5F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("flare_gun"), new Parameters(4.0F, 0.33F, 0.0F, 0.2F)),
        Map.entry(Reference.id("grenade_launcher"), new Parameters(3.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("hollenfire_mk2"), new Parameters(4.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("holy_shotgun"), new Parameters(8.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("hypersonic_cannon"), new Parameters(4.0F, 0.55F, 0.0F, 0.2F)),
        Map.entry(Reference.id("infantry_rifle"), new Parameters(3.0F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("light_machine_gun"), new Parameters(1.0F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("minigun"), new Parameters(0.8F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("primitive_bow"), new Parameters(2.0F, 0.1F, 0.0F, 0.2F)),
        Map.entry(Reference.id("pump_shotgun"), new Parameters(10.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("repeating_shotgun"), new Parameters(10.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("revolver"), new Parameters(4.0F, 0.33F, 0.0F, 0.2F)),
        Map.entry(Reference.id("rocket_launcher"), new Parameters(7.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("semi_auto_pistol"), new Parameters(2.0F, 0.33F, 0.0F, 0.2F)),
        Map.entry(Reference.id("semi_auto_rifle"), new Parameters(3.0F, 0.15F, 0.0F, 0.2F)),
        Map.entry(Reference.id("service_rifle"), new Parameters(4.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("soulhunter_mk2"), new Parameters(2.0F, 0.25F, 0.0F, 0.2F)),
        Map.entry(Reference.id("subsonic_rifle"), new Parameters(1.3F, 0.09F, 0.0F, 0.2F)),
        Map.entry(Reference.id("supersonic_shotgun"), new Parameters(7.0F, 0.2F, 0.0F, 0.2F)),
        Map.entry(Reference.id("typhoonee"), new Parameters(8.0F, 0.5F, 0.0F, 0.2F)),
        Map.entry(Reference.id("waterpipe_shotgun"), new Parameters(10.0F, 0.5F, 0.0F, 0.2F))
    );

    private RecoilProfiles() {}

    public static float multiplier(ResourceLocation id) {
        return MULTIPLIERS.getOrDefault(id, 1.0F);
    }

    public static Parameters parameters(ResourceLocation id) {
        return PARAMETERS.getOrDefault(id, NONE);
    }
}
