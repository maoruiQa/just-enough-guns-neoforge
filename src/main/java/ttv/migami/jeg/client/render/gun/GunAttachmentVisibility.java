package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.Reference;

public final class GunAttachmentVisibility {
    private static final Set<String> DEFAULT_HIDDEN_ATTACHMENT_BONES = Set.of(
            "attachment_bone",
            "scope",
            "railing",
            "iron_sight",
            "modified_iron_sight",
            "stock_iron_sight",
            "hidden_iron_sight",
            "silencer",
            "extended_mag",
            "extended_mag_2",
            "extended_magazine",
            "drum_mag",
            "drum_mag_2",
            "drum_magazine",
            "light_stock",
            "tactical_stock",
            "weighted_stock",
            "makeshift_stock",
            "under_barrel",
            "grip",
            "light_grip",
            "vertical_grip",
            "angled_grip",
            "light_handguard",
            "tactical_handguard",
            "weighted_handguard",
            "light_hg_grip",
            "tactical_hg_grip",
            "weighted_hg_grip",
            "ejector",
            "bolt",
            "chamber"
    );

    private static final Map<ResourceLocation, Rule> RULES = Map.ofEntries(
            rule(Reference.id("service_rifle"),
                    Set.of("railing", "iron_sight", "modified_iron_sight", "stock_iron_sight", "handguard", "light_handguard"),
                    Set.of("tactical_handguard", "weighted_handguard", "light_hg_grip", "tactical_hg_grip", "weighted_hg_grip"))
    );

    private GunAttachmentVisibility() {
    }

    public static void apply(ResourceLocation gunId, GeoBone bone) {
        String boneName = bone.getName();
        Rule rule = RULES.get(gunId);
        if (rule != null) {
            Boolean hidden = rule.visibility(boneName);
            if (hidden != null) {
                bone.setHidden(hidden);
                return;
            }
        }

        if (DEFAULT_HIDDEN_ATTACHMENT_BONES.contains(boneName)) {
            bone.setHidden(true);
        }
    }

    private static Map.Entry<ResourceLocation, Rule> rule(
            ResourceLocation gunId,
            Set<String> visible,
            Set<String> hidden
    ) {
        return Map.entry(gunId, new Rule(visible, hidden));
    }

    private record Rule(Set<String> visible, Set<String> hidden) {
        private Boolean visibility(String boneName) {
            if (visible.contains(boneName)) {
                return false;
            }
            if (hidden.contains(boneName)) {
                return true;
            }
            return null;
        }
    }
}
