package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import ttv.migami.jeg.gun.GunStats;

public final class GunHandTransform {
    private GunHandTransform() {}

    public static void apply(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, GunStats stats, float partialTick) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float recoil = GunRecoilHandler.getRecoil(partialTick);

        String gunId = stats.id().getPath();
        boolean isFingerGun = gunId.equals("finger_gun");
        boolean isBow = gunId.contains("bow");
        boolean isShortWeapon = (!isBow) && (gunId.contains("pistol") || gunId.contains("revolver") ||
                gunId.contains("grenade_launcher") || gunId.contains("flare_gun") ||
                gunId.contains("double_barrel") || gunId.contains("waterpipe"));
        boolean isDoubleBarrel = gunId.contains("double_barrel");
        boolean isTyphoonee = gunId.contains("typhoonee");

        if (isFingerGun) {
            poseStack.translate(direction * 0.75F, -0.48F, -0.35F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 0.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        } else if (player.isCrouching()) {
            float xOffset;
            float yOffset;
            float zOffset;
            float aimRotation;

            if (isBow) {
                xOffset = 0.2915F;
                yOffset = 0.247F;
                zOffset = -0.56F;
                aimRotation = 0.5F;
            } else if (isTyphoonee) {
                xOffset = 0.35F;
                yOffset = -0.7F;
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
            float xOffset;
            float yOffset;

            if (isBow) {
                xOffset = 0.46F;
                yOffset = 0.2F;
            } else if (isTyphoonee) {
                xOffset = 0.65F;
                yOffset = -0.5F;
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
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 3.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 0.5F));
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));
        poseStack.scale(1.25F, 1.25F, 1.25F);

        if (recoil > 0.0001F) {
            poseStack.translate(direction * recoil * 0.01F, -recoil * 0.02F, -recoil * 0.08F);
            poseStack.mulPose(Axis.XP.rotationDegrees(recoil * -4.5F));
        }

    }
}
