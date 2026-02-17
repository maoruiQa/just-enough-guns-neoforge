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
        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);

        String gunId = stats.id().getPath();
        boolean isFingerGun = gunId.equals("finger_gun");
        boolean isBow = gunId.contains("bow");
        boolean isFlareGun = gunId.contains("flare_gun");
        boolean isPistolClass = gunId.contains("pistol") || gunId.contains("revolver");
        boolean isCombatRifle = gunId.contains("combat_rifle");
        boolean isShortWeapon = (!isBow) && (gunId.contains("pistol") || gunId.contains("revolver")
                || gunId.contains("grenade_launcher") || gunId.contains("flare_gun")
                || gunId.contains("double_barrel") || gunId.contains("waterpipe"));
        boolean isRocketLauncher = gunId.contains("rocket_launcher");
        boolean isDoubleBarrel = gunId.contains("double_barrel");
        boolean isTyphoonee = gunId.contains("typhoonee");

        float hipX;
        float hipY;
        float hipZ;
        float adsX;
        float adsY;
        float adsZ;
        float adsYaw;
        float hipYaw = 3.0F;
        float scale = 1.25F;

        if (isFingerGun) {
            hipX = 0.75F; hipY = -0.48F; hipZ = -0.35F;
            adsX = 0.75F; adsY = -0.48F; adsZ = -0.35F;
            hipYaw = 180.0F; adsYaw = 180.0F; scale = 1.25F;
        } else if (isRocketLauncher) {
            // Keep rocket launcher on shoulder in both hip-fire and ADS.
            hipX = 0.76F; hipY = 0.42F; hipZ = -0.72F;
            adsX = 0.44F; adsY = 0.48F; adsZ = -1.02F;
            hipYaw = -1.0F; adsYaw = -2.0F; scale = 1.48F;
        } else if (isBow) {
            hipX = 0.46F; hipY = 0.20F; hipZ = -0.60F;
            adsX = 0.2915F; adsY = 0.247F; adsZ = -0.56F; adsYaw = 0.5F;
        } else if (isTyphoonee) {
            hipX = 0.65F; hipY = -0.50F; hipZ = -0.60F;
            adsX = 0.35F; adsY = -0.70F; adsZ = -0.90F; adsYaw = 5.0F;
        } else if (isDoubleBarrel) {
            hipX = 0.40F; hipY = 0.36F; hipZ = -0.66F;
            adsX = 0.24F; adsY = 0.12F; adsZ = -0.94F; adsYaw = -1.8F;
            hipYaw = -1.2F; scale = 1.36F;
        } else if (isShortWeapon) {
            hipX = 0.44F; hipY = 0.34F; hipZ = -0.64F;
            adsX = 0.24F; adsY = 0.14F; adsZ = -0.82F; adsYaw = -1.6F;
            hipYaw = -0.8F; scale = 1.33F;
        } else {
            hipX = 0.66F; hipY = 0.22F; hipZ = -0.66F;
            adsX = 0.28F; adsY = 0.10F; adsZ = -0.88F; adsYaw = -2.0F;
            hipYaw = -1.0F; scale = 1.35F;
        }

        float x = Mth.lerp(ads, hipX, adsX);
        // Shift all guns slightly left while ADS.
        x -= ads * 0.02F;
        if (isRocketLauncher) {
            // Rocket launcher: extremely slight right shift while ADS.
            x += ads * 0.03F;
        }
        if (isPistolClass) {
            // Pistols/revolvers: nudge ADS position back to the right.
            x += ads * 0.04F;
        }
        if (isTyphoonee || isFlareGun) {
            // Slight right correction for Typhoonee and flare gun.
            x += ads * 0.04F;
        }
        float y = Mth.lerp(ads, hipY, adsY);
        // All guns: slight upward lift while ADS.
        y += ads * 0.05F;
        if (!isRocketLauncher) {
            // Non-rocket guns: additional medium-small ADS lift.
            y += ads * 0.07F;
        }
        float z = Mth.lerp(ads, hipZ, adsZ);
        float yaw = Mth.lerp(ads, hipYaw, adsYaw);
        if (!isFingerGun) {
            // Apply global leftward orientation mainly while ADS.
            yaw += 1.2F * ads;
        }
        if (isPistolClass) {
            // Fix pistol class drift: slight right in hip-fire, stronger left in ADS.
            yaw += Mth.lerp(ads, -0.3F, 1.0F);
        }
        if (isTyphoonee || isFlareGun) {
            // Typhoonee/flare issues are ADS-specific; keep hip-fire mostly unaffected.
            yaw += 0.6F * ads;
        }

        poseStack.translate(direction * x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));

        poseStack.scale(scale, scale, scale);

        float equip = Mth.clamp(equipProcess, 0.0F, 1.0F);
        poseStack.translate(0.0F, -0.6F * equip, 0.0F);

        if (!isBow) {
            float swingRoot = Mth.sqrt(swingProcess);
            float swingX = -0.3F * Mth.sin(swingRoot * (float) Math.PI);
            float swingY = 0.4F * Mth.sin(swingRoot * ((float) Math.PI * 2F));
            poseStack.translate(direction * swingX * 0.08F, swingY * 0.05F, 0.0F);
        }

        return true;
    }
}
