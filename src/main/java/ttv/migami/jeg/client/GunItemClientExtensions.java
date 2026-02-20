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
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GunItem;

public final class GunItemClientExtensions implements IClientItemExtensions {
    private static final float ADS_YAW_ALIGNED = 0.0F;
    private static final float HIP_PITCH = 4.0F;
    private static final float ADS_PITCH_ALIGNED = 0.0F;
    private static final float ADS_LIFT_Y = 0.03F;

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
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        String gunPath = stats.id().getPath();
        boolean isBow = gunPath.contains("bow");
        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        GunPoseProfile profile = GunPoseProfile.forGun(stats.id());

        float xOffset = Mth.lerp(ads, profile.hipX(), profile.adsX());
        float yOffset = Mth.lerp(ads, profile.hipY(), profile.adsY());
        float zOffset = Mth.lerp(ads, profile.hipZ(), profile.adsZ());
        float yaw = Mth.lerp(ads, profile.hipYaw(), ADS_YAW_ALIGNED);
        float pitch = Mth.lerp(ads, HIP_PITCH, ADS_PITCH_ALIGNED);

        poseStack.translate(direction * xOffset, yOffset + ads * ADS_LIFT_Y, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 0.5F * (1.0F - ads)));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(profile.scale(), profile.scale(), profile.scale());

        // Keep vanilla-like equip/swing movement to reduce "hard snap" while switching items.
        float equip = Mth.clamp(equipProcess, 0.0F, 1.0F);
        poseStack.translate(0.0F, -0.6F * equip, 0.0F);
        if (!isBow) {
            float swingRoot = Mth.sqrt(swingProcess);
            float swingX = -0.3F * Mth.sin(swingRoot * (float) Math.PI);
            float swingY = 0.4F * Mth.sin(swingRoot * ((float) Math.PI * 2F));
            poseStack.translate(direction * swingX * 0.08F, swingY * 0.05F, 0.0F);
        }

        float recoil = GunRecoilHandler.getRecoil(partialTick);
        if (Math.abs(recoil) > 0.0001F) {
            // Vertical model shake only: no forward tilt and no forward/back translation.
            poseStack.translate(0.0F, -recoil * 0.1F, 0.0F);
        }

        return true;
    }
}
