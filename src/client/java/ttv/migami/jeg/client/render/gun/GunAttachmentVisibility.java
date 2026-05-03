package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
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

    private static final Map<Identifier, Rule> RULES = Map.ofEntries(
            rule(Reference.id("service_rifle"),
                    Set.of("railing", "iron_sight", "modified_iron_sight", "stock_iron_sight", "handguard", "light_handguard"),
                    Set.of("tactical_handguard", "weighted_handguard", "light_hg_grip", "tactical_hg_grip", "weighted_hg_grip")),
            rule(Reference.id("revolver"),
                    Set.of("chamber"),
                    Set.of())
    );

    private GunAttachmentVisibility() {}

    public static void apply(Identifier gunId, GeoBone bone) {
        apply(gunId, bone.name(), bone.frameSnapshot);
    }

    public static void apply(Identifier gunId, BoneSnapshots snapshots) {
        Rule rule = RULES.get(gunId);
        if (rule != null) {
            rule.visible().forEach(boneName -> snapshots.ifPresent(boneName, snapshot -> snapshot.skipRender(false)));
            rule.hidden().forEach(boneName -> snapshots.ifPresent(boneName, snapshot -> snapshot.skipRender(true)));
        }

        DEFAULT_HIDDEN_ATTACHMENT_BONES.forEach(boneName -> {
            if (rule == null || !rule.visible().contains(boneName)) {
                snapshots.ifPresent(boneName, snapshot -> snapshot.skipRender(true));
            }
        });
    }

    private static void apply(Identifier gunId, String boneName, BoneSnapshot snapshot) {
        Rule rule = RULES.get(gunId);
        if (rule != null) {
            Boolean hidden = rule.visibility(boneName);
            if (hidden != null) {
                snapshot.skipRender(hidden);
                return;
            }
        }

        if (DEFAULT_HIDDEN_ATTACHMENT_BONES.contains(boneName)) {
            snapshot.skipRender(true);
        }
    }

    private static Map.Entry<Identifier, Rule> rule(Identifier gunId, Set<String> visible, Set<String> hidden) {
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
