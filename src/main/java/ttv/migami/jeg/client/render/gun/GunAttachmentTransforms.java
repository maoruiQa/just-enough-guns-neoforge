package ttv.migami.jeg.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.attachment.AttachmentType;

public final class GunAttachmentTransforms {
    private static final double UNIT = 1.0D / 16.0D;
    private static final Map<ResourceLocation, Map<AttachmentType, Transform>> TRANSFORMS = Map.ofEntries(
            entry("abstract_gun", scope(0.0D, 4.715D, 5.0D), barrel(0.0D, 3.97D, -4.5D), underBarrel(0.0D, 2.7D, 0.3D), special(-1.0D, 4.0D, 0.0D)),
            entry("assault_rifle", scope(0.0D, 4.715D, 5.0D), barrel(0.0D, 3.97D, -4.5D), stock(0.0D, 3.0D, 8.1D), underBarrel(0.0D, 2.7D, 0.3D), special(-1.0D, 4.0D, 0.0D)),
            entry("blossom_rifle", scope(0.0D, 6.0D, 3.0D), barrel(0.0D, 5.1D, -9.4D), underBarrel(0.0D, 3.9D, 0.0D), special(-1.1D, 5.0D, -6.2D)),
            entry("bolt_action_rifle", scope(0.0D, 5.0D, -4.4D), barrel(0.0D, 4.905D, -14.5D), special(-1.0D, 4.0D, -7.0D)),
            entry("burst_rifle", scope(0.0D, 5.73D, 5.4D), barrel(0.0D, 4.34D, -5.5D), stock(0.0D, 3.0D, 8.1D), underBarrel(0.0D, 2.975D, -0.78D), special(-1.2D, 4.335D, -2.0D)),
            entry("combat_pistol", barrel(0.0D, 5.5D, -3.2D), special(-0.8D, 5.2D, 0.0D)),
            entry("combat_rifle", scope(0.0D, 5.885D, 4.8D), barrel(0.0D, 4.585D, -7.855D), stock(0.0D, 3.0D, 8.1D), underBarrel(0.0D, 0.0D, 0.0D), special(-1.2D, 4.63D, -1.8D)),
            entry("custom_smg", barrel(0.0D, 4.475D, -1.2D), special(-0.8D, 3.8D, 0.0D)),
            entry("grenade_launcher", scope(0.0D, 4.5D, 0.0D), underBarrel(0.0D, 2.5D, -2.5D)),
            entry("hollenfire_mk2", scope(0.0D, 5.305D, 4.5D), stock(0.0D, 3.0D, 8.1D), underBarrel(0.0D, 2.725D, -1.0D), special(-1.9D, 4.7D, -4.0D)),
            entry("holy_shotgun", scope(0.0D, 2.97D, 5.0D), barrel(0.0D, 2.804D, -5.5D), underBarrel(0.0D, 1.77D, 2.125D, 0.0D)),
            entry("infantry_rifle", scope(0.0D, 4.97D, -0.715D), barrel(0.0D, 4.5D, -9.6D), special(-1.0D, 4.3D, -6.0D)),
            entry("light_machine_gun", scope(0.0D, 5.33D, 0.0D), barrel(0.0D, 4.855D, -7.0D), underBarrel(0.0D, 2.5D, 0.3D), special(-1.2D, 3.63D, -0.1D)),
            entry("minigun", special(0.0D, 1.2D, 4.0D)),
            entry("phantom_smg", barrel(0.0D, 4.475D, -1.2D), special(-0.8D, 3.8D, 0.0D)),
            entry("pump_shotgun", scope(0.0D, 4.265D, 0.8D), barrel(0.0D, 4.125D, -5.51D), underBarrel(0.0D, 0.0D, 0.0D), special(-1.0D, 4.0D, -2.0D)),
            entry("repeating_shotgun", scope(0.0D, 4.9D, 0.0D), barrel(0.0D, 4.65D, -10.0D), underBarrel(0.0D, 3.0D, 0.45D), special(-0.8D, 4.0D, -5.0D)),
            entry("revolver", barrel(0.0D, 4.69D, -3.2D)),
            entry("semi_auto_pistol", barrel(0.0D, 5.4D, -3.2D), stock(0.0D, 4.0D, 2.0D), special(-0.8D, 5.2D, 0.0D)),
            entry("semi_auto_rifle", scope(0.0D, 4.5D, 5.4D), barrel(0.0D, 4.425D, -5.5D), underBarrel(0.0D, 3.6D, -2.8D), special(-0.8D, 4.0D, 2.0D)),
            entry("service_rifle", scope(0.0D, 5.305D, 4.5D), barrel(0.0D, 4.685D, -8.5D), stock(0.0D, 3.0D, 8.1D), underBarrel(0.0D, 3.3D, -1.0D), special(-1.2D, 4.7D, -3.5D)),
            entry("soulhunter_mk2", scope(0.0D, 5.305D, 1.5D), underBarrel(0.0D, 3.3D, -4.33D)),
            entry("subsonic_rifle", scope(0.0D, 4.9D, 5.0D), barrel(0.0D, 4.515D, -8.25D), underBarrel(0.0D, 3.15D, -1.0D)),
            entry("supersonic_shotgun", scope(0.0D, 5.195D, 4.5D), barrel(0.0D, 4.125D, -5.51D), underBarrel(0.0D, 0.0D, 0.0D)),
            entry("waterpipe_shotgun", stock(0.0D, 3.0D, 8.1D), special(0.0D, 3.0D, -5.0D))
    );

    private GunAttachmentTransforms() {
    }

    public static Optional<Transform> transform(ResourceLocation gunId, AttachmentType type) {
        return Optional.ofNullable(TRANSFORMS.get(gunId)).map(slots -> slots.get(type));
    }

    private static Map.Entry<ResourceLocation, Map<AttachmentType, Transform>> entry(String gunId, SlotTransform... transforms) {
        EnumMap<AttachmentType, Transform> slots = new EnumMap<>(AttachmentType.class);
        for (SlotTransform transform : transforms) {
            slots.put(transform.type(), transform.transform());
        }
        return Map.entry(Reference.id(gunId), Map.copyOf(slots));
    }

    private static SlotTransform barrel(double x, double y, double z) {
        return barrel(x, y, z, 1.0D);
    }

    private static SlotTransform barrel(double x, double y, double z, double scale) {
        return slot(AttachmentType.BARREL, x, y, z, scale);
    }

    private static SlotTransform scope(double x, double y, double z) {
        return slot(AttachmentType.SCOPE, x, y, z, 1.0D);
    }

    private static SlotTransform stock(double x, double y, double z) {
        return slot(AttachmentType.STOCK, x, y, z, 1.0D);
    }

    private static SlotTransform underBarrel(double x, double y, double z) {
        return underBarrel(x, y, z, 1.0D);
    }

    private static SlotTransform underBarrel(double x, double y, double z, double scale) {
        return slot(AttachmentType.UNDER_BARREL, x, y, z, scale);
    }

    private static SlotTransform special(double x, double y, double z) {
        return slot(AttachmentType.SPECIAL, x, y, z, 1.0D);
    }

    private static SlotTransform slot(AttachmentType type, double x, double y, double z, double scale) {
        return new SlotTransform(type, new Transform(x, y, z, scale));
    }

    private record SlotTransform(AttachmentType type, Transform transform) {
    }

    public record Transform(double x, double y, double z, double scale) {
        public boolean isVisible() {
            return this.scale > 0.0D;
        }

        public void apply(PoseStack poseStack) {
            poseStack.translate(this.x * UNIT, (this.y - 8.0D) * UNIT, this.z * UNIT);
            poseStack.scale((float) this.scale, (float) this.scale, (float) this.scale);
        }
    }
}
