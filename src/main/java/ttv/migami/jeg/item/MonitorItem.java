package ttv.migami.jeg.item;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.init.ModDataComponents;

public final class MonitorItem extends Item {
    public MonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            String linked = stack.get(ModDataComponents.DRONE_LINK.get());
            if (linked == null) {
                player.displayClientMessage(Component.translatable("message.jeg.drone.not_linked"), true);
                return InteractionResultHolder.fail(stack);
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
                player.displayClientMessage(Component.translatable("message.jeg.drone.lost"), true);
                return InteractionResultHolder.fail(stack);
            }
            if (stack.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false)) {
                drone.stopControl(serverPlayer);
            } else {
                drone.startControl(serverPlayer, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
