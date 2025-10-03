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

        // Check if it's a short weapon (pistol, grenade launcher)
        String gunId = stats.id().getPath();
        boolean isShortWeapon = gunId.contains("pistol") || gunId.contains("revolver") ||
                                gunId.contains("grenade_launcher") || gunId.contains("flare_gun");

        if (player.isCrouching()) {
            // Aiming down sights
            float xOffset = isShortWeapon ? 0.25F : 0.35F;
            float yOffset = isShortWeapon ? -0.05F : -0.1F;
            poseStack.translate(direction * xOffset, yOffset, -1.0F);
            // Very little rotation for straight forward aim
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 5.0F));
        } else {
            // Hip fire - closer to camera for larger screen presence
            float xOffset = isShortWeapon ? 0.65F : 0.85F;
            float yOffset = isShortWeapon ? 0.0F : -0.05F;
            poseStack.translate(direction * xOffset, yOffset, -0.6F);
            // Minimal rotation to keep gun pointing forward
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 10.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 3.0F));
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
