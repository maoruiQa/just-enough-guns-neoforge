package ttv.migami.jeg.item;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.init.ModDataComponents;

public final class MonitorItem extends Item {
    public MonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // SW only uses main hand monitor for control
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            String linked = stack.get(ModDataComponents.DRONE_LINK.get());
            if (linked == null || linked.isEmpty() || "none".equals(linked)) {
                player.sendSystemMessage(Component.translatable("message.jeg.drone.not_linked"));
                return InteractionResult.FAIL;
            }
            Entity entity;
            try {
                entity = serverLevel.getEntity(UUID.fromString(linked));
            } catch (IllegalArgumentException ignored) {
                entity = null;
            }
            if (!(entity instanceof DroneEntity drone) || !drone.isAlive()) {
                stack.remove(ModDataComponents.DRONE_LINK.get());
                stack.remove(ModDataComponents.DRONE_CONTROLLING.get());
                player.sendSystemMessage(Component.translatable("message.jeg.drone.lost"));
                return InteractionResult.FAIL;
            }
            if (stack.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false)) {
                drone.stopControl(serverPlayer);
            } else {
                drone.startControl(serverPlayer, stack);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean isSelected = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!isSelected && stack.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false)) {
            stack.remove(ModDataComponents.DRONE_CONTROLLING.get());
            if (player instanceof ServerPlayer serverPlayer) {
                String linked = stack.get(ModDataComponents.DRONE_LINK.get());
                if (linked != null) {
                    try {
                        Entity drone = level.getEntity(UUID.fromString(linked));
                        if (drone instanceof DroneEntity droneEntity) {
                            droneEntity.stopControl(serverPlayer);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        // Update last known position for tooltip
        String linked = stack.get(ModDataComponents.DRONE_LINK.get());
        if (linked != null) {
            try {
                Entity drone = level.getEntity(UUID.fromString(linked));
                if (drone instanceof DroneEntity) {
                    stack.set(ModDataComponents.DRONE_POS_X.get(), (float) drone.getX());
                    stack.set(ModDataComponents.DRONE_POS_Y.get(), (float) drone.getY());
                    stack.set(ModDataComponents.DRONE_POS_Z.get(), (float) drone.getZ());
                }
            } catch (Exception ignored) {
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        tooltipAdder.accept(Component.translatable("des.jeg.monitor.howto").withStyle(ChatFormatting.GRAY));
        String linked = stack.get(ModDataComponents.DRONE_LINK.get());
        if (linked == null || linked.isEmpty()) {
            tooltipAdder.accept(Component.translatable("des.jeg.monitor.unlinked").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipAdder.accept(Component.translatable("des.jeg.monitor.linked").withStyle(ChatFormatting.GREEN));
        Float x = stack.get(ModDataComponents.DRONE_POS_X.get());
        Float y = stack.get(ModDataComponents.DRONE_POS_Y.get());
        Float z = stack.get(ModDataComponents.DRONE_POS_Z.get());
        if (x == null || y == null || z == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            double dist = player.position().distanceTo(new Vec3(x, y, z));
            tooltipAdder.accept(Component.translatable("des.jeg.monitor.distance", String.format("%.1fm", dist)).withStyle(ChatFormatting.GRAY));
        }
        tooltipAdder.accept(Component.literal(String.format("X: %.1f Y: %.1f Z: %.1f", x, y, z)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
