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
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;

public final class SpecialExplosiveItem extends Item {
    public enum Kind { C4, CLAYMORE, TM_62 }

    private final Kind kind;

    public SpecialExplosiveItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 20);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            boolean remoteOrFuse = switch (this.kind) {
                case C4 -> stack.getOrDefault(ModDataComponents.C4_REMOTE.get(), false);
                case TM_62 -> player.isShiftKeyDown();
                case CLAYMORE -> false;
            };
            PlacedExplosiveEntity entity = PlacedExplosiveEntity.throwFrom(serverLevel, player, this.kind, remoteOrFuse);
            if (serverLevel.addFreshEntity(entity)) {
                if (this.kind == Kind.C4) {
                    var throwSound = ModSounds.ALL.get(Reference.id("item.c4.throw"));
                    if (throwSound == null) {
                        throwSound = ModSounds.ALL.get(Reference.id("item.c4.beep"));
                    }
                    if (throwSound != null && player instanceof ServerPlayer serverPlayer) {
                        serverLevel.playSound(null, serverPlayer.blockPosition(), throwSound.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (this.kind == Kind.C4) {
            if (stack.getOrDefault(ModDataComponents.C4_REMOTE.get(), false)) {
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.control").withStyle(ChatFormatting.GREEN));
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.control.stats").withStyle(ChatFormatting.DARK_GREEN));
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.control.howto").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.time").withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.time.stats").withStyle(ChatFormatting.DARK_GRAY));
                tooltipComponents.add(Component.translatable("des.jeg.c4_bomb.time.howto").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (this.kind == Kind.TM_62) {
            tooltipComponents.add(Component.translatable("des.jeg.tm_62.fuse").withStyle(ChatFormatting.GRAY));
        } else if (this.kind == Kind.CLAYMORE) {
            tooltipComponents.add(Component.translatable("des.jeg.claymore").withStyle(ChatFormatting.GRAY));
        }
    }
}
