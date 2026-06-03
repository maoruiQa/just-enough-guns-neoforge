package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;

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
            rule(Reference.id("combat_rifle"),
                    Set.of("iron_sight"),
                    Set.of("hidden_iron_sight")),
            rule(Reference.id("service_rifle"),
                    Set.of("railing", "iron_sight", "modified_iron_sight", "stock_iron_sight", "handguard", "light_handguard"),
                    Set.of("tactical_handguard", "weighted_handguard", "light_hg_grip", "tactical_hg_grip", "weighted_hg_grip")),
            rule(Reference.id("revolver"),
                    Set.of("chamber"),
                    Set.of())
    );

    private GunAttachmentVisibility() {
    }

    public static void apply(ResourceLocation gunId, ItemStack stack, GeoBone bone) {
        String boneName = bone.getName();
        if (Reference.id("bolt_action_rifle").equals(gunId)) {
            applyBoltActionRifle(stack, bone, boneName);
            return;
        }

        Boolean attachmentVisibility = installedAttachmentVisibility(stack, boneName);
        if (attachmentVisibility != null) {
            bone.setHidden(attachmentVisibility);
            return;
        }

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

    private static void applyBoltActionRifle(ItemStack stack, GeoBone bone, String boneName) {
        boolean scoped = GunScopeSupport.isBoltActionRifleScopeEnabled(stack);
        if ("attachment_bone".equals(boneName)) {
            bone.setHidden(!scoped);
            return;
        }
        if ("iron_sight".equals(boneName)) {
            bone.setHidden(scoped);
            return;
        }
        if (DEFAULT_HIDDEN_ATTACHMENT_BONES.contains(boneName)) {
            bone.setHidden(true);
        }
    }

    private static Boolean installedAttachmentVisibility(ItemStack stack, String boneName) {
        if (isScopeBone(boneName)) {
            return !GunAttachments.has(stack, AttachmentType.SCOPE);
        }
        if ("iron_sight".equals(boneName) || "modified_iron_sight".equals(boneName) || "stock_iron_sight".equals(boneName)) {
            return GunAttachments.has(stack, AttachmentType.SCOPE);
        }
        if (isInstalled(stack, AttachmentType.BARREL, boneName)) {
            return false;
        }
        if (isInstalled(stack, AttachmentType.STOCK, boneName)) {
            return false;
        }
        if (isInstalled(stack, AttachmentType.UNDER_BARREL, boneName)) {
            return false;
        }
        if (isDefaultMagazineBone(boneName)) {
            return GunAttachments.has(stack, AttachmentType.MAGAZINE);
        }
        if (isInstalled(stack, AttachmentType.MAGAZINE, boneName)) {
            return false;
        }
        if (isInstalled(stack, AttachmentType.SPECIAL, boneName)) {
            return false;
        }
        if (isGenericInstalledBone(stack, boneName)) {
            return false;
        }
        return null;
    }

    private static boolean isScopeBone(String boneName) {
        return "attachment_bone".equals(boneName) || "scope".equals(boneName) || "railing".equals(boneName);
    }

    private static boolean isDefaultMagazineBone(String boneName) {
        return "default_mag".equals(boneName) || "default_mag_2".equals(boneName);
    }

    private static boolean isInstalled(ItemStack stack, AttachmentType type, String boneName) {
        return GunAttachments.id(stack, type)
                .map(id -> id.getPath().equals(boneName))
                .orElse(false);
    }

    private static boolean isGenericInstalledBone(ItemStack stack, String boneName) {
        return switch (boneName) {
            case "under_barrel", "grip" -> GunAttachments.has(stack, AttachmentType.UNDER_BARREL);
            case "extended_mag_2", "extended_magazine" -> isInstalled(stack, AttachmentType.MAGAZINE, "extended_mag");
            case "drum_mag_2", "drum_magazine" -> isInstalled(stack, AttachmentType.MAGAZINE, "drum_mag");
            case "light_handguard", "light_hg_grip" -> isInstalled(stack, AttachmentType.STOCK, "light_stock");
            case "tactical_handguard", "tactical_hg_grip" -> isInstalled(stack, AttachmentType.STOCK, "tactical_stock");
            case "weighted_handguard", "weighted_hg_grip" -> isInstalled(stack, AttachmentType.STOCK, "weighted_stock");
            default -> false;
        };
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
