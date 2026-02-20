package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonGunArmRenderEvents {
    private FirstPersonGunArmRenderEvents() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // NeoForge 1.21.11 fix:
        // This event-based arm overlay caused *all* held items (e.g. wheat seeds) to show an extra
        // pair of forward-stretched arms. Gun arms are rendered via GeckoLib bones in
        // GunFirstPersonArmsLayer, so we must not render anything here.
        return;
    }

    private static void applyVanillaFirstPersonArmTransform(PoseStack poseStack, float equipProgress, float swingProgress, HumanoidArm arm) {
        boolean right = arm != HumanoidArm.LEFT;
        float f = right ? 1.0F : -1.0F;

        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);

        poseStack.translate(f * (f2 + 0.64000005F), f3 - 0.6F + equipProgress * -0.6F, f4 - 0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));

        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));

        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);
    }
}
