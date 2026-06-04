package ttv.migami.jeg.item.attachment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;

public final class GunAttachmentRules {
    private static final Map<ResourceLocation, Set<AttachmentType>> SUPPORTED_TYPES = Map.ofEntries(
            support("abstract_gun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("assault_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("blossom_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("bolt_action_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.SPECIAL),
            support("burst_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("combat_pistol", AttachmentType.BARREL, AttachmentType.SPECIAL),
            support("combat_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("custom_smg", AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("double_barrel_shotgun", AttachmentType.STOCK),
            support("grenade_launcher", AttachmentType.SCOPE, AttachmentType.UNDER_BARREL),
            support("hollenfire_mk2", AttachmentType.SCOPE, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("holy_shotgun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL),
            support("infantry_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("light_machine_gun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL, AttachmentType.SPECIAL),
            support("minigun", AttachmentType.SPECIAL),
            support("phantom_smg", AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("pump_shotgun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.SPECIAL),
            support("repeating_shotgun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL, AttachmentType.SPECIAL),
            support("revolver", AttachmentType.BARREL, AttachmentType.STOCK),
            support("semi_auto_pistol", AttachmentType.BARREL, AttachmentType.SPECIAL),
            support("semi_auto_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("service_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.STOCK, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE, AttachmentType.SPECIAL),
            support("soulhunter_mk2", AttachmentType.SCOPE, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE),
            support("subsonic_rifle", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL, AttachmentType.MAGAZINE),
            support("supersonic_shotgun", AttachmentType.SCOPE, AttachmentType.BARREL, AttachmentType.UNDER_BARREL),
            support("waterpipe_shotgun", AttachmentType.SPECIAL)
    );

    private GunAttachmentRules() {
    }

    public static boolean canAttach(ItemStack gunStack, AttachmentType type) {
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return false;
        }
        if (type.isCosmetic()) {
            return true;
        }
        return SUPPORTED_TYPES.getOrDefault(gun.getStats().id(), Collections.emptySet()).contains(type);
    }

    private static Map.Entry<ResourceLocation, Set<AttachmentType>> support(String gunId, AttachmentType... types) {
        return Map.entry(Reference.id(gunId), Set.of(types));
    }
}
