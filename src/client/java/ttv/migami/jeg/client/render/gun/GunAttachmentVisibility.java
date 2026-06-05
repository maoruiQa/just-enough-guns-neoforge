package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModDataComponents;

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
            "default_mag",
            "default_mag_2",
            "magazine_default",
            "mag_default",
            "extended_mag",
            "extended_mag_2",
            "extended_magazine",
            "magazine_extended",
            "mag_extended",
            "drum_mag",
            "drum_mag_2",
            "drum_magazine",
            "magazine_drum",
            "mag_drum",
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

        if (Config.magazineFeedEnabled()) {
            Boolean magazineVisibility = magazineItemVisibility(stack, boneName);
            if (magazineVisibility != null) {
                bone.setHidden(magazineVisibility);
                return;
            }
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
        if ("attachment_bone".equals(boneName)) {
            bone.setHidden(!GunScopeSupport.isBoltActionRifleScopeEnabled());
            return;
        }
        if ("iron_sight".equals(boneName)) {
            bone.setHidden(GunScopeSupport.isBoltActionRifleScopeEnabled());
            return;
        }
        if (Config.magazineFeedEnabled()) {
            Boolean magazineVisibility = magazineItemVisibility(stack, boneName);
            if (magazineVisibility != null) {
                bone.setHidden(magazineVisibility);
                return;
            }
        }
        if (DEFAULT_HIDDEN_ATTACHMENT_BONES.contains(boneName)) {
            bone.setHidden(true);
        }
    }

    private static Boolean magazineItemVisibility(ItemStack stack, String boneName) {
        if (!isMagazineBone(boneName)) {
            return null;
        }

        MagazineVisualType type = currentMagazineVisualType(stack);
        return switch (type) {
            case DEFAULT -> !isDefaultMagazineBone(boneName);
            case EXTENDED -> !isExtendedMagazineBone(boneName);
            case DRUM -> !isDrumMagazineBone(boneName);
        };
    }

    private static MagazineVisualType currentMagazineVisualType(ItemStack stack) {
        int totalTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), 0);
        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (totalTicks > 0 && remainingTicks > 0) {
            int elapsedTicks = Math.max(0, totalTicks - remainingTicks);
            String reloadMagazine = elapsedTicks < totalTicks / 2
                    ? stack.get(ModDataComponents.GUN_RELOAD_FROM_MAGAZINE_ITEM.get())
                    : stack.get(ModDataComponents.GUN_RELOAD_TO_MAGAZINE_ITEM.get());
            MagazineVisualType type = magazineVisualType(reloadMagazine);
            if (type != null) {
                return type;
            }
        }

        MagazineVisualType loadedType = magazineVisualType(stack.get(ModDataComponents.GUN_LOADED_MAGAZINE_ITEM.get()));
        return loadedType != null ? loadedType : MagazineVisualType.DEFAULT;
    }

    private static MagazineVisualType magazineVisualType(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return null;
        }

        String path = id.getPath();
        if (path.endsWith("_drum_magazine")) {
            return MagazineVisualType.DRUM;
        }
        if (path.endsWith("_extended_magazine")) {
            return MagazineVisualType.EXTENDED;
        }
        if (path.endsWith("_magazine")) {
            return MagazineVisualType.DEFAULT;
        }
        return null;
    }

    private static boolean isMagazineBone(String boneName) {
        return isDefaultMagazineBone(boneName) || isExtendedMagazineBone(boneName) || isDrumMagazineBone(boneName);
    }

    private static boolean isDefaultMagazineBone(String boneName) {
        return "default_mag".equals(boneName)
                || "default_mag_2".equals(boneName)
                || "magazine_default".equals(boneName)
                || "mag_default".equals(boneName);
    }

    private static boolean isExtendedMagazineBone(String boneName) {
        return "extended_mag".equals(boneName)
                || "extended_mag_2".equals(boneName)
                || "extended_magazine".equals(boneName)
                || "magazine_extended".equals(boneName)
                || "mag_extended".equals(boneName);
    }

    private static boolean isDrumMagazineBone(String boneName) {
        return "drum_mag".equals(boneName)
                || "drum_mag_2".equals(boneName)
                || "drum_magazine".equals(boneName)
                || "magazine_drum".equals(boneName)
                || "mag_drum".equals(boneName);
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

    private enum MagazineVisualType {
        DEFAULT,
        EXTENDED,
        DRUM
    }
}
