package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GunItem;

public final class GunItemClientExtensions implements IClientItemExtensions {
    private final GunStats stats;

    public GunItemClientExtensions(GunItem item) {
        this.stats = item.getStats();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;

        float equip = Mth.clamp(equipProcess, 0.0F, 1.0F);
        float recoil = GunRecoilHandler.getRecoil(partialTick);

        // Check if it's a short weapon (pistol, grenade launcher, shotgun)
        String gunId = stats.id().getPath();
        boolean isBow = gunId.contains("bow");
        boolean isShortWeapon = (!isBow) && (gunId.contains("pistol") || gunId.contains("revolver") ||
                                gunId.contains("grenade_launcher") || gunId.contains("flare_gun") ||
                                gunId.contains("double_barrel") || gunId.contains("waterpipe"));
        // Extra large display for double barrel shotgun
        boolean isDoubleBarrel = gunId.contains("double_barrel");
        // Special handling for typhoonee (large heavy weapon)
        boolean isTyphoonee = gunId.contains("typhoonee");

        if (player.isCrouching()) {
            // Aiming down sights - short weapons closer to face and more to the right
            float xOffset, yOffset, zOffset, aimRotation;

            if (isBow) {
                xOffset = 0.2915F;
                yOffset = 0.247F;
                zOffset = -0.56F;
                aimRotation = 0.5F;
            } else if (isTyphoonee) {
                // Typhoonee needs special positioning when aiming - centered and lower
                xOffset = 0.35F;
                yOffset = -0.7F;  // Even lower for proper center positioning
                zOffset = -0.9F;
                aimRotation = 5.0F;
            } else if (isDoubleBarrel) {
                xOffset = 0.2F;
                yOffset = 0.25F;
                zOffset = -1.0F;
                aimRotation = 5.0F;
            } else if (isShortWeapon) {
                xOffset = 0.3F;
                yOffset = 0.2F;
                zOffset = -0.8F;
                aimRotation = 2.0F;
            } else {
                xOffset = 0.32F;
                yOffset = -0.02F;
                zOffset = -0.82F;
                aimRotation = 4.0F;
            }

            poseStack.translate(direction * xOffset, yOffset, zOffset);
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * aimRotation));
        } else {
            // Hip fire - short weapons occupy more screen space, closer to camera
            float xOffset, yOffset;

            if (isBow) {
                xOffset = 0.46F;
                yOffset = 0.2F;
            } else if (isTyphoonee) {
                // Typhoonee needs centered and lower position for hip fire
                xOffset = 0.65F;  // Standard weapon horizontal offset
                yOffset = -0.5F;  // Lower for proper center positioning
            } else if (isDoubleBarrel) {
                xOffset = 0.3F;
                yOffset = 0.28F;
            } else if (isShortWeapon) {
                xOffset = 0.35F;
                yOffset = 0.25F;
            } else {
                xOffset = 0.58F;
                yOffset = 0.1F;
            }

            poseStack.translate(direction * xOffset, yOffset, -0.6F);
            // Minimal rotation so barrel points toward crosshair
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 3.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 0.5F));
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));
        poseStack.scale(1.25F, 1.25F, 1.25F);

        if (recoil > 0.0001F) {
            poseStack.translate(direction * recoil * 0.01F, -recoil * 0.02F, -recoil * 0.08F);
            poseStack.mulPose(Axis.XP.rotationDegrees(recoil * -4.5F));
        }

        return true;
    }
}
