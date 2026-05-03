package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GunItem;

public final class GunItemClientExtensions implements IClientItemExtensions {
    private static final float FIRST_PERSON_GLOBAL_SCALE_MULTIPLIER = 1.28F;
    private final GunStats stats;

    public GunItemClientExtensions(GunItem item) {
        this.stats = item.getStats();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        // Match NeoForge 1.21.10: third-person two-handed hold pose for guns.
        // First-person pose is handled by applyForgeHandTransform + GeckoLib arms layer for AnimatedGunItem.
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
        return applyForStats(this.stats, poseStack, player, arm, partialTick, equipProcess, swingProcess);
    }

    public static boolean applyForStats(
            GunStats stats,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        String gunPath = stats.id().getPath();
        boolean isBow = gunPath.contains("bow");
        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        GunPoseProfile profile = GunPoseProfile.forGun(stats.id());

        float xOffset = Mth.lerp(ads, profile.hipX(), profile.adsX());
        float yOffset = Mth.lerp(ads, profile.hipY(), profile.adsY());
        float zOffset = Mth.lerp(ads, profile.hipZ(), profile.adsZ());
        float yaw = Mth.lerp(ads, profile.hipYaw(), profile.adsYaw());
        float adsWeight = ads * ads;

        boolean isRocketLauncher = "rocket_launcher".equals(gunPath);
        boolean isTyphoonee = "typhoonee".equals(gunPath);
        float adsScreenXShift;
        float adsDownShift;
        float adsForwardShift;
        if (isTyphoonee) {
            adsScreenXShift = 0.03F;
            adsDownShift = -2.05F;
            adsForwardShift = 0.24F;
        } else if (isRocketLauncher) {
            adsScreenXShift = 0.09F;
            adsDownShift = -0.24F;
            adsForwardShift = 0.00F;
        } else {
            adsScreenXShift = -0.30F;
            adsDownShift = -0.36F;
            adsForwardShift = 0.00F;
        }

        boolean mediumDownGroup =
                "hollenfire_mk2".equals(gunPath)
                        || "semi_auto_pistol".equals(gunPath)
                        || "combat_pistol".equals(gunPath)
                        || "combat_rifle".equals(gunPath)
                        || "flamethrower".equals(gunPath);
        if (mediumDownGroup) {
            adsDownShift -= 0.18F;
        }
        if ("service_rifle".equals(gunPath)) {
            adsDownShift -= 0.12F;
        }

        xOffset += direction * adsScreenXShift * adsWeight;
        yOffset += adsDownShift * adsWeight;
        zOffset += adsForwardShift * adsWeight;

        poseStack.translate(direction * xOffset, yOffset, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 0.5F * (1.0F - ads)));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(adsWeight, 4.0F, 0.8F)));
        float firstPersonScale = profile.scale() * FIRST_PERSON_GLOBAL_SCALE_MULTIPLIER;
        poseStack.scale(firstPersonScale, firstPersonScale, firstPersonScale);

        // Keep vanilla-like equip/swing movement to reduce "hard snap" while switching items.
        float equip = Mth.clamp(equipProcess, 0.0F, 1.0F);
        poseStack.translate(0.0F, -0.6F * equip, 0.0F);
        if (!isBow) {
            float swingRoot = Mth.sqrt(swingProcess);
            float swingX = -0.3F * Mth.sin(swingRoot * (float) Math.PI);
            float swingY = 0.4F * Mth.sin(swingRoot * ((float) Math.PI * 2F));
            poseStack.translate(direction * swingX * 0.08F, swingY * 0.05F, 0.0F);
        }

        applyBobbingTransforms(poseStack, player, partialTick, ads);
        applySwayTransforms(poseStack, player, partialTick, ads);
        applySprintingTransforms(poseStack, player, direction, ads);
        applyRecoilTransforms(poseStack, ads);
        return true;
    }

    private static void applyBobbingTransforms(PoseStack poseStack, Player player, float partialTick, float ads) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.bobView().get() || minecraft.getCameraEntity() != player) {
            return;
        }

        if (player.onGround() && !player.isPassenger()) {
            float speed = (float) player.getDeltaMovement().horizontalDistance();
            float time = (player.tickCount + partialTick) * 0.5F;
            float bobAmount = Mth.sin(time) * speed * 0.15F;
            float adsDamping = 1.0F - ads * 0.7F;
            poseStack.translate(0.0D, bobAmount * adsDamping, 0.0D);
        }
    }

    private static void applySwayTransforms(PoseStack poseStack, LocalPlayer player, float partialTick, float ads) {
        if (player.onGround() && !player.isPassenger()) {
            float speed = (float) player.getDeltaMovement().horizontalDistance();
            float time = (player.tickCount + partialTick) * 0.3F;
            float swayX = Mth.sin(time) * speed * 2.0F;
            float swayZ = Mth.cos(time * 0.7F) * speed * 2.0F;
            float adsDamping = 1.0F - ads * 0.8F;
            poseStack.mulPose(Axis.XP.rotationDegrees(swayX * adsDamping));
            poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ * adsDamping));
        }

        float fallDelta = (float) Mth.clamp(player.yo - player.getY(), -1.0D, 1.0D);
        fallDelta *= (1.0F - ads * 0.5F) * (1.0F - Mth.abs(player.getXRot()) / 90.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(fallDelta * 12.0F));
    }

    private static void applySprintingTransforms(PoseStack poseStack, LocalPlayer player, int direction, float ads) {
        if (!player.isSprinting() || AimingHandler.get().isAiming()) {
            return;
        }

        float transition = 1.0F - ads;
        poseStack.translate(-0.18F * direction * transition, -0.08F * transition, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(30.0F * direction * transition));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * transition));
    }

    private static void applyRecoilTransforms(PoseStack poseStack, float ads) {
        double recoilNormal = GunRecoilHandler.getGunRecoilNormal();
        if (recoilNormal <= 0.0D) {
            return;
        }

        float adsReduction = GunRecoilHandler.getAdsRecoilReduction();
        double kick = GunRecoilHandler.getGunRecoilKick() * 0.0625D * recoilNormal * adsReduction;
        float recoilLift = (float) (GunRecoilHandler.getGunRecoilAngle() * recoilNormal) * adsReduction;
        float recoilSwayAmount = 2.0F + 1.0F * (1.0F - ads);
        float recoilSway = (float) ((GunRecoilHandler.getGunRecoilRandom() * recoilSwayAmount - recoilSwayAmount / 2.0F) * recoilNormal);
        poseStack.translate(0.0D, 0.0D, kick);
        poseStack.translate(0.0D, 0.0D, 0.15D);
        poseStack.mulPose(Axis.YP.rotationDegrees(recoilSway));
        poseStack.mulPose(Axis.ZP.rotationDegrees(recoilSway));
        poseStack.mulPose(Axis.XP.rotationDegrees(recoilLift));
        poseStack.translate(0.0D, 0.0D, -0.15D);
    }
}
