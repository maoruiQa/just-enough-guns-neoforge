package ttv.migami.jeg.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModSounds;

public final class DetonatorItem extends Item {
    public DetonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 10);

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            var click = ModSounds.ALL.get(Reference.id("item.c4.detonator"));
            if (click != null) {
                serverLevel.playSound(null, serverPlayer.blockPosition(), click.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            // Infinite AABB is rejected by entity lookups - scan all loaded entities like SW.
            int detonated = 0;
            for (var entity : serverLevel.getAllEntities()) {
                if (entity instanceof PlacedExplosiveEntity explosive && explosive.isRemoteC4OwnedBy(player.getUUID())) {
                    explosive.detonate();
                    detonated++;
                }
            }
            // Worn C4 vests owned by this player only
            detonated += C4VestItem.detonateOwnedWornVests(serverLevel, serverPlayer);

            if (detonated > 0) {
                player.awardStat(Stats.ITEM_USED.get(this));
                player.displayClientMessage(Component.translatable("message.jeg.detonator.detonated", detonated), true);
            } else {
                player.displayClientMessage(Component.translatable("message.jeg.detonator.none"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.jeg.detonator").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.jeg.detonator.howto").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("des.jeg.detonator.vest").withStyle(ChatFormatting.DARK_GRAY));
    }
}
