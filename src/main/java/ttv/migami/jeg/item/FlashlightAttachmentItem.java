package ttv.migami.jeg.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;

public class FlashlightAttachmentItem extends AttachmentItem {
    private static final int COOLDOWN_TICKS = 20;
    private static final int CHARGE_AMOUNT = 40;

    public FlashlightAttachmentItem(AttachmentType type, AttachmentModifiers modifiers, Properties properties) {
        super(type, modifiers, properties);
    }

    public static boolean isPowered(ItemStack stack) {
        return stack.getItem() instanceof FlashlightAttachmentItem
                && Boolean.TRUE.equals(stack.get(ModDataComponents.GUN_FLASHLIGHT_POWERED.get()));
    }

    public static void charge(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof FlashlightAttachmentItem)) {
            return;
        }
        int battery = ensureBattery(stack);
        setBattery(stack, battery + CHARGE_AMOUNT);
        player.level().gameEvent(player, GameEvent.NOTE_BLOCK_PLAY, player.position());
        playSound(player.level(), player, Reference.id("item.flashlight_charge"), 1.0F, randomChargePitch(player));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (!Config.allowFlashlights()) {
                Component message = Component.translatable("chat.jeg.disabled_flashlights").withStyle(ChatFormatting.GRAY);
                player.displayClientMessage(message, true);
                return InteractionResultHolder.success(stack);
            }

            int battery = ensureBattery(stack);
            boolean powered = isPowered(stack);
            if (battery <= 0) {
                if (powered) {
                    setPowered(stack, false);
                    playSound(level, player, Reference.id("item.goose"), 1.0F, 1.0F);
                }
                Component message = Component.translatable("chat.jeg.flashlight_battery_dead").withStyle(ChatFormatting.RED);
                player.displayClientMessage(message, true);
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS / 2);
                return InteractionResultHolder.success(stack);
            }

            if (!powered && !player.getAbilities().instabuild && !player.isSpectator()) {
                setBattery(stack, battery - 1);
            }
            setPowered(stack, !powered);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, player.position());
            playSound(level, player, Reference.id("item.flashlight"), 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        int battery = ensureBattery(stack);
        if (isPowered(stack) && !player.getAbilities().instabuild && !player.isSpectator()) {
            if (battery > 0) {
                setBattery(stack, battery - 1);
            } else {
                setPowered(stack, false);
            }
        }

        if (selected && battery <= 0) {
            Component message = Component.translatable("chat.jeg.flashlight_battery_dead").withStyle(ChatFormatting.RED);
            player.displayClientMessage(message, true);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return battery(stack) < GunAttachments.FLASHLIGHT_MAX_BATTERY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * battery(stack) / GunAttachments.FLASHLIGHT_MAX_BATTERY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float percentage = (float) battery(stack) / GunAttachments.FLASHLIGHT_MAX_BATTERY;
        if (percentage > 0.5F) {
            int red = (int) (255 * (1 - 2 * (percentage - 0.5F)));
            return (red << 16) | 0xFF00;
        }
        int green = (int) (255 * 2 * percentage);
        return (0xFF << 16) | (green << 8);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.jeg.tooltip_item.flashlight").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("info.jeg.tooltip_item.flashlight_help").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.jeg.tooltip_item.flashlight_help_gun", Component.translatable("key.jeg.melee")).withStyle(ChatFormatting.GRAY));
    }

    private static int ensureBattery(ItemStack stack) {
        Integer stored = stack.get(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get());
        if (stored == null) {
            setBattery(stack, GunAttachments.FLASHLIGHT_MAX_BATTERY);
            return GunAttachments.FLASHLIGHT_MAX_BATTERY;
        }
        int clamped = Math.clamp(stored, 0, GunAttachments.FLASHLIGHT_MAX_BATTERY);
        if (clamped != stored) {
            setBattery(stack, clamped);
        }
        return clamped;
    }

    private static int battery(ItemStack stack) {
        return Math.clamp(
                stack.getOrDefault(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get(), GunAttachments.FLASHLIGHT_MAX_BATTERY),
                0,
                GunAttachments.FLASHLIGHT_MAX_BATTERY
        );
    }

    private static void setBattery(ItemStack stack, int battery) {
        stack.set(
                ModDataComponents.GUN_FLASHLIGHT_BATTERY.get(),
                Math.clamp(battery, 0, GunAttachments.FLASHLIGHT_MAX_BATTERY)
        );
    }

    private static void setPowered(ItemStack stack, boolean powered) {
        stack.set(ModDataComponents.GUN_FLASHLIGHT_POWERED.get(), powered);
    }

    private static float randomChargePitch(Player player) {
        return 0.9F + player.getRandom().nextFloat() * 0.25F;
    }

    private static void playSound(Level level, Player player, ResourceLocation id, float volume, float pitch) {
        SoundEvent sound = ModSounds.ALL.get(id) != null ? ModSounds.ALL.get(id).get() : null;
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        }
    }
}
