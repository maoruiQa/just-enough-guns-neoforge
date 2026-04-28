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
    private static final float ADS_EXTRA_Y_ALL_OTHERS = 0.060F;
    private static final float ADS_EXTRA_Y_STABLE = 0.050F;
    private static final float ADS_EXTRA_Y_SHOTGUN_AND_CUSTOM_SMG = 0.075F;
    private static final float ADS_EXTRA_Y_SUPERSONIC_SHOTGUN = 0.045F;
    // 1.21.1 renderer baseline sits lower than 1.21.11 for the same profile numbers.
    // Per-gun compensation values are anchored by combat_rifle parity and applied
    // after aligning first-person arm rendering to the 1.21.11-style bone-driven path.
    private record LegacyComp(float yHip, float yAds, float z) {}

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
        float[] comp = legacyCompensation(stats, ads);
        yOffset += comp[0];
        zOffset += comp[1];
        float yaw = Mth.lerp(ads, profile.hipYaw(), ADS_YAW_ALIGNED);
        float pitch = Mth.lerp(ads, HIP_PITCH, ADS_PITCH_ALIGNED);

        poseStack.translate(direction * xOffset, yOffset + ads * ADS_LIFT_Y + adsExtraHeight(gunPath, ads), zOffset);
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

        applyRecoilTransforms(poseStack, ads);
        return true;
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

    private static float[] legacyCompensation(GunStats stats, float ads) {
        LegacyComp comp = legacyCompFor(stats.id().getPath());
        float y = Mth.lerp(ads, comp.yHip(), comp.yAds());
        return new float[] { y, comp.z() };
    }

    private static float adsExtraHeight(String gunPath, float ads) {
        if (ads <= 0.0F) {
            return 0.0F;
        }
        float extra = switch (gunPath) {
            case "combat_rifle" -> 0.0F;
            // Keep these weapons at their current tuned ADS height.
            case "hollenfire_mk2", "infantry_rifle", "blossom_rifle", "subsonic_rifle", "soulhunter_mk2" -> ADS_EXTRA_Y_STABLE;
            case "service_rifle" -> 0.020F;
            // Raise ADS for shotgun family and custom_smg.
            case "double_barrel_shotgun", "holy_shotgun", "pump_shotgun", "repeating_shotgun", "waterpipe_shotgun", "shotgun", "custom_smg", "phantom_smg" ->
                    ADS_EXTRA_Y_SHOTGUN_AND_CUSTOM_SMG;
            // Medium-small ADS raise for supersonic_shotgun.
            case "supersonic_shotgun" -> ADS_EXTRA_Y_SUPERSONIC_SHOTGUN;
            // Requested small ADS raise for burst_rifle.
            case "burst_rifle" -> 0.030F;
            // Keep previous downward/low-uplift retunes.
            case "flamethrower", "light_machine_gun" -> 0.020F;
            // All other guns: slight ADS uplift.
            default -> ADS_EXTRA_Y_ALL_OTHERS;
        };
        return ads * extra;
    }

    private static LegacyComp legacyCompFor(String gunPath) {
        if ("minigun".equals(gunPath)) {
            // Keep minigun ADS net height stable after global ADS uplift.
            return new LegacyComp(0.58F, 0.57F, -0.06F);
        }
        if ("rocket_launcher".equals(gunPath)) {
            // Slight ADS height increase after global ADS uplift.
            return new LegacyComp(0.59F, 0.66F, -0.04F);
        }
        if ("typhoonee".equals(gunPath)) {
            // Medium downshift to reduce top-of-screen clipping.
            return new LegacyComp(0.98F, 1.12F, -0.06F);
        }
        if ("soulhunter_mk2".equals(gunPath)) {
            // Medium hip downshift while keeping ADS net height near previous level.
            return new LegacyComp(0.12F, 0.17F, -0.04F);
        }
        if ("burst_rifle".equals(gunPath)) {
            // Medium-large ADS downshift for burst_rifle while preserving hip stance.
            return new LegacyComp(0.60F, 0.68F, -0.03F);
        }
        return switch (gunPath) {
            // SMG
            case "custom_smg", "phantom_smg" -> new LegacyComp(0.58F, 0.70F, -0.02F);
            // Pistol
            case "combat_pistol", "semi_auto_pistol", "revolver", "finger_gun" -> new LegacyComp(0.58F, 0.70F, -0.02F);
            // Sniper
            case "bolt_action_rifle", "semi_auto_rifle" -> new LegacyComp(0.64F, 0.76F, -0.05F);
            // LMG
            case "light_machine_gun", "hollenfire_mk2", "flamethrower" -> new LegacyComp(0.59F, 0.71F, -0.04F);
            // Shotgun
            case "double_barrel_shotgun", "holy_shotgun", "pump_shotgun", "repeating_shotgun", "supersonic_shotgun", "waterpipe_shotgun" ->
                    new LegacyComp(0.62F, 0.74F, -0.04F);
            // Heavy
            case "grenade_launcher", "hypersonic_cannon" -> new LegacyComp(0.58F, 0.70F, -0.06F);
            // Rifle
            case "assault_rifle", "blossom_rifle", "burst_rifle", "combat_rifle", "infantry_rifle", "service_rifle", "subsonic_rifle", "abstract_gun" ->
                    new LegacyComp(0.60F, 0.72F, -0.03F);
            // Special/default
            case "flare_gun" -> new LegacyComp(0.58F, 0.70F, -0.02F);
            default -> new LegacyComp(0.60F, 0.72F, -0.03F);
        };
    }
}
