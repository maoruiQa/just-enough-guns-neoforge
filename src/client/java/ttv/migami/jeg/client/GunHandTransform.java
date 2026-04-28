package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import ttv.migami.jeg.gun.GunStats;

public final class GunHandTransform {
    private GunHandTransform() {}

    public static void apply(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, GunStats stats, float partialTick) {
        GunItemClientExtensions.applyForStats(stats, poseStack, player, arm, partialTick, 0.0F, 0.0F);
    }
}
