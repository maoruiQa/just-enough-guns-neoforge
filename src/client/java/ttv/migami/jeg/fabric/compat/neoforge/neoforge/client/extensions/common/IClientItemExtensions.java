package ttv.migami.jeg.fabric.compat.neoforge.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IClientItemExtensions {
    Map<Item, IClientItemExtensions> REGISTRY = new ConcurrentHashMap<>();
    IClientItemExtensions EMPTY = new IClientItemExtensions() {};

    static void register(Item item, IClientItemExtensions extensions) {
        if (item != null && extensions != null) {
            REGISTRY.put(item, extensions);
        }
    }

    static IClientItemExtensions of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY;
        }
        return REGISTRY.getOrDefault(stack.getItem(), EMPTY);
    }

    default HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.EMPTY;
    }

    default boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        return false;
    }
}
