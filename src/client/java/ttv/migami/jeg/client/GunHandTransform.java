package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import ttv.migami.jeg.gun.GunStats;

public final class GunHandTransform {
    private GunHandTransform() {}

    public static boolean apply(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, GunStats stats, float partialTick, float equipProcess, float swingProcess) {
        return GunItemClientExtensions.applyForStats(stats, poseStack, player, arm, partialTick, equipProcess, swingProcess);
    }
}
