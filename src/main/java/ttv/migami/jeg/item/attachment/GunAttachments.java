package ttv.migami.jeg.item.attachment;

import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.AttachmentItem;
import ttv.migami.jeg.item.GunItem;

public final class GunAttachments {
    public static final int FLASHLIGHT_MAX_BATTERY = 600;
    private static final ResourceLocation EXPLOSIVE_MUZZLE = Reference.id("explosive_muzzle");
    private static final ResourceLocation MAKESHIFT_STOCK = Reference.id("makeshift_stock");
    private static final Set<ResourceLocation> MAGAZINE_CAPACITY_ATTACHMENTS = Set.of(
            Reference.id("extended_mag"),
            Reference.id("drum_mag")
    );
    private static final Set<ResourceLocation> MAKESHIFT_STOCK_GUNS = Set.of(
            Reference.id("assault_rifle"),
            Reference.id("custom_smg"),
            Reference.id("double_barrel_shotgun"),
            Reference.id("phantom_smg"),
            Reference.id("pump_shotgun"),
            Reference.id("revolver"),
            Reference.id("semi_auto_pistol"),
            Reference.id("semi_auto_rifle"),
            Reference.id("waterpipe_shotgun")
    );
    private static final AttachmentModifiers SPYGLASS_SCOPE_MODIFIERS = AttachmentModifiers.builder()
            .aimFovModifier(0.2F)
            .adsSpeedMultiplier(0.76D)
            .adsViewOffset(0.0D, 1.0D, -3.15D)
            .build();

    private GunAttachments() {
    }

    public enum FlashlightToggleResult {
        MISSING,
        DEAD,
        TOGGLED
    }

    public static boolean has(ItemStack gunStack, AttachmentType type) {
        return stack(gunStack, type).isPresent() || id(gunStack, type).isPresent();
    }

    public static Optional<ResourceLocation> id(ItemStack gunStack, AttachmentType type) {
        ItemStack stored = stackFromComponent(gunStack, type);
        if (!stored.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stored.getItem());
            if (id != null) {
                return Optional.of(id);
            }
        }
        String raw = gunStack.get(component(type));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(raw));
    }

    public static Optional<ItemStack> stack(ItemStack gunStack, AttachmentType type) {
        ItemStack stored = stackFromComponent(gunStack, type);
        if (!stored.isEmpty()) {
            return Optional.of(stored.copyWithCount(1));
        }
        return idFromComponent(gunStack, type)
                .flatMap(BuiltInRegistries.ITEM::getOptional)
                .map(ItemStack::new);
    }

    public static void updateStoredStack(ItemStack gunStack, AttachmentType type, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            clear(gunStack, type);
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(attachmentStack.getItem());
        if (id != null) {
            gunStack.set(component(type), id.toString());
            gunStack.set(stackComponent(type), new StoredAttachmentStack(attachmentStack));
        }
    }

    public static Optional<AttachmentItem> item(ItemStack gunStack, AttachmentType type) {
        return stack(gunStack, type)
                .map(ItemStack::getItem)
                .filter(AttachmentItem.class::isInstance)
                .map(AttachmentItem.class::cast);
    }

    public static Optional<Item> cosmeticItem(ItemStack gunStack, AttachmentType type) {
        if (!type.isCosmetic()) {
            return Optional.empty();
        }
        return stack(gunStack, type).map(ItemStack::getItem);
    }

    public static AttachmentModifiers modifiers(ItemStack gunStack, AttachmentType type) {
        Optional<AttachmentItem> attachment = item(gunStack, type);
        if (attachment.isPresent()) {
            return attachment.get().modifiers();
        }
        if (type == AttachmentType.SCOPE && stack(gunStack, type).map(ItemStack::getItem).filter(SpyglassItem.class::isInstance).isPresent()) {
            return SPYGLASS_SCOPE_MODIFIERS;
        }
        return AttachmentModifiers.NONE;
    }

    public static AttachmentModifiers modifiers(ItemStack gunStack) {
        float aimFovModifier = 0.0F;
        float damageMultiplier = 1.0F;
        float spreadMultiplier = 1.0F;
        float recoilMultiplier = 1.0F;
        float kickMultiplier = 1.0F;
        double adsSpeedMultiplier = 1.0D;
        double adsViewXOffset = 0.0D;
        double adsViewYOffset = 0.0D;
        double adsViewZOffset = 0.0D;
        double magazineCapacityMultiplier = 1.0D;
        double fireSoundRadiusMultiplier = 1.0D;
        boolean silenced = false;
        boolean explosiveAmmo = false;
        boolean flashlight = false;
        boolean laserPointer = false;
        boolean annoying = false;
        boolean increasedJamming = false;

        for (AttachmentType type : AttachmentType.values()) {
            AttachmentModifiers modifier = modifiers(gunStack, type);
            if (modifier.aimFovModifier() > 0.0F) {
                aimFovModifier = modifier.aimFovModifier();
            }
            damageMultiplier *= modifier.damageMultiplier();
            spreadMultiplier *= modifier.spreadMultiplier();
            recoilMultiplier *= modifier.recoilMultiplier();
            kickMultiplier *= modifier.kickMultiplier();
            adsSpeedMultiplier *= modifier.adsSpeedMultiplier();
            adsViewXOffset += modifier.adsViewXOffset();
            adsViewYOffset += modifier.adsViewYOffset();
            adsViewZOffset += modifier.adsViewZOffset();
            magazineCapacityMultiplier *= modifier.magazineCapacityMultiplier();
            fireSoundRadiusMultiplier = Math.min(fireSoundRadiusMultiplier, modifier.fireSoundRadiusMultiplier());
            silenced |= modifier.silenced();
            explosiveAmmo |= modifier.explosiveAmmo();
            flashlight |= modifier.flashlight();
            laserPointer |= modifier.laserPointer();
            annoying |= modifier.annoying();
            increasedJamming |= modifier.increasedJamming();
        }

        return new AttachmentModifiers(
                aimFovModifier,
                damageMultiplier,
                spreadMultiplier,
                recoilMultiplier,
                kickMultiplier,
                adsSpeedMultiplier,
                adsViewXOffset,
                adsViewYOffset,
                adsViewZOffset,
                magazineCapacityMultiplier,
                fireSoundRadiusMultiplier,
                silenced,
                explosiveAmmo,
                flashlight,
                laserPointer,
                annoying,
                increasedJamming
        );
    }

    public static boolean set(ItemStack gunStack, AttachmentType type, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            clear(gunStack, type);
            return true;
        }
        if (type.isCosmetic()) {
            return setCosmetic(gunStack, type, attachmentStack);
        }
        Item item = attachmentStack.getItem();
        if (!canAttachStack(gunStack, type, attachmentStack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        gunStack.set(component(type), id.toString());
        gunStack.set(stackComponent(type), new StoredAttachmentStack(attachmentStack));
        removeDamage(gunStack, type);
        if (item instanceof AttachmentItem attachment && type == AttachmentType.SPECIAL) {
            if (attachment.modifiers().flashlight()) {
                ensureFlashlightBattery(gunStack);
            } else {
                removeFlashlightState(gunStack);
            }
        }
        return true;
    }

    public static boolean setCosmetic(ItemStack gunStack, AttachmentType type, ItemStack attachmentStack) {
        if (!type.isCosmetic()) {
            return false;
        }
        if (attachmentStack.isEmpty()) {
            clear(gunStack, type);
            return true;
        }
        if (!isCosmeticStack(type, attachmentStack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(attachmentStack.getItem());
        if (id == null) {
            return false;
        }
        gunStack.set(component(type), id.toString());
        gunStack.set(stackComponent(type), new StoredAttachmentStack(attachmentStack));
        return true;
    }

    public static boolean canAttachStack(ItemStack gunStack, AttachmentType type, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            return true;
        }
        if (type.isCosmetic()) {
            return isCosmeticStack(type, attachmentStack);
        }
        Item item = attachmentStack.getItem();
        if (type == AttachmentType.STOCK && isMakeshiftStock(item) && canUseMakeshiftStock(gunStack)) {
            return true;
        }
        if (!GunAttachmentRules.canAttach(gunStack, type)) {
            return false;
        }
        if (type == AttachmentType.MAGAZINE && Config.magazineFeedEnabled() && isMagazineCapacityAttachment(item)) {
            return false;
        }
        if (item instanceof AttachmentItem attachment) {
            return attachment.type() == type && attachment.canAttachTo(gunStack);
        }
        return isPseudoAttachmentStack(type, attachmentStack);
    }

    private static boolean isMagazineCapacityAttachment(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && MAGAZINE_CAPACITY_ATTACHMENTS.contains(id);
    }

    public static boolean canUseAttachmentSlot(ItemStack gunStack, AttachmentType type) {
        if (type == AttachmentType.STOCK && canUseMakeshiftStock(gunStack)) {
            return true;
        }
        return GunAttachmentRules.canAttach(gunStack, type);
    }

    public static boolean isCosmeticStack(AttachmentType type, ItemStack stack) {
        return switch (type) {
            case PAINT_JOB -> stack.getItem() instanceof PaintJobCanItem;
            case DYE -> stack.getItem() instanceof net.minecraft.world.item.DyeItem;
            case KILL_EFFECT -> stack.getItem() instanceof KillEffectItem;
            default -> false;
        };
    }

    public static boolean isPseudoAttachmentStack(AttachmentType type, ItemStack stack) {
        return switch (type) {
            case SCOPE -> stack.getItem() instanceof SpyglassItem;
            case BARREL -> stack.getItem() instanceof SwordItem;
            default -> false;
        };
    }

    private static boolean isMakeshiftStock(Item item) {
        return MAKESHIFT_STOCK.equals(BuiltInRegistries.ITEM.getKey(item));
    }

    private static boolean canUseMakeshiftStock(ItemStack gunStack) {
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return false;
        }
        return GunAttachmentRules.canAttach(gunStack, AttachmentType.STOCK)
                || MAKESHIFT_STOCK_GUNS.contains(gun.getStats().id());
    }

    private static boolean isExplosiveMuzzle(ItemStack stack) {
        return EXPLOSIVE_MUZZLE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean hasMending(Player player, ItemStack stack) {
        HolderLookup.RegistryLookup<Enchantment> lookup = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return lookup.get(Enchantments.MENDING)
                .map((Holder.Reference<Enchantment> holder) -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }

    public static void clear(ItemStack gunStack, AttachmentType type) {
        gunStack.remove(component(type));
        gunStack.remove(stackComponent(type));
        removeDamage(gunStack, type);
        if (type == AttachmentType.SPECIAL) {
            removeFlashlightState(gunStack);
        }
    }

    public static boolean hasFlashlight(ItemStack gunStack) {
        return item(gunStack, AttachmentType.SPECIAL)
                .map(AttachmentItem::modifiers)
                .map(AttachmentModifiers::flashlight)
                .orElse(false);
    }

    public static boolean isFlashlightPowered(ItemStack gunStack) {
        return hasFlashlight(gunStack) && Boolean.TRUE.equals(gunStack.get(ModDataComponents.GUN_FLASHLIGHT_POWERED.get()));
    }

    public static boolean areMedalsEnabled(ItemStack gunStack) {
        return Boolean.TRUE.equals(gunStack.get(ModDataComponents.GUN_MEDALS_ENABLED.get()));
    }

    public static boolean toggleMedals(ItemStack gunStack) {
        boolean enabled = !areMedalsEnabled(gunStack);
        gunStack.set(ModDataComponents.GUN_MEDALS_ENABLED.get(), enabled);
        return enabled;
    }

    public static FlashlightToggleResult toggleFlashlight(ItemStack gunStack, Player player) {
        if (!hasFlashlight(gunStack)) {
            return FlashlightToggleResult.MISSING;
        }
        int battery = ensureFlashlightBattery(gunStack);
        boolean powered = isFlashlightPowered(gunStack);
        if (!powered && battery <= 0) {
            gunStack.set(ModDataComponents.GUN_FLASHLIGHT_POWERED.get(), false);
            return FlashlightToggleResult.DEAD;
        }
        if (!powered && player != null && !player.getAbilities().instabuild && !player.isSpectator()) {
            setFlashlightBattery(gunStack, battery - 1);
        }
        gunStack.set(ModDataComponents.GUN_FLASHLIGHT_POWERED.get(), !powered);
        return FlashlightToggleResult.TOGGLED;
    }

    public static boolean tickFlashlightBattery(ItemStack gunStack, Player player) {
        if (!isFlashlightPowered(gunStack)) {
            return false;
        }
        int battery = ensureFlashlightBattery(gunStack);
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return true;
        }
        if (battery <= 0) {
            gunStack.set(ModDataComponents.GUN_FLASHLIGHT_POWERED.get(), false);
            Component message = Component.translatable("chat.jeg.flashlight_battery_dead").withStyle(ChatFormatting.RED);
            player.displayClientMessage(message, true);
            return false;
        }
        setFlashlightBattery(gunStack, battery - 1);
        return true;
    }

    public static void damageOnShot(ItemStack gunStack, Level level, Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (hasMending(player, gunStack)) {
            return;
        }
        damageOnShot(gunStack, AttachmentType.SCOPE, level, player);
        damageOnShot(gunStack, AttachmentType.BARREL, level, player);
        damageOnShot(gunStack, AttachmentType.STOCK, level, player);
        damageOnShot(gunStack, AttachmentType.UNDER_BARREL, level, player);
    }

    private static void damageOnShot(ItemStack gunStack, AttachmentType type, Level level, Player player) {
        Optional<ItemStack> storedAttachment = stack(gunStack, type);
        if (storedAttachment.isEmpty()) {
            removeDamage(gunStack, type);
            return;
        }

        ItemStack attachmentStack = storedAttachment.get();
        if (!(attachmentStack.getItem() instanceof AttachmentItem)) {
            removeDamage(gunStack, type);
            return;
        }
        if (!attachmentStack.isDamageableItem()) {
            removeDamage(gunStack, type);
            return;
        }
        if (hasMending(player, attachmentStack) && !isExplosiveMuzzle(attachmentStack)) {
            return;
        }

        DataComponentType<Integer> damageComponent = damageComponent(type);
        if (damageComponent == null) {
            return;
        }

        int maxDamage = attachmentStack.getMaxDamage();
        int currentDamage = Math.max(0, gunStack.getOrDefault(damageComponent, 0));
        currentDamage = Math.max(currentDamage, attachmentStack.getDamageValue());
        if (currentDamage >= maxDamage - 1) {
            level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            clear(gunStack, type);
            Component message = Component.translatable("chat.jeg.attachment_broke").withStyle(ChatFormatting.GRAY);
            player.displayClientMessage(message, true);
        } else {
            attachmentStack.setDamageValue(currentDamage + 1);
            gunStack.set(stackComponent(type), new StoredAttachmentStack(attachmentStack));
            gunStack.set(damageComponent, currentDamage + 1);
        }
    }

    private static ItemStack stackFromComponent(ItemStack gunStack, AttachmentType type) {
        StoredAttachmentStack stored = gunStack.get(stackComponent(type));
        return stored == null ? ItemStack.EMPTY : stored.toItemStack();
    }

    private static Optional<ResourceLocation> idFromComponent(ItemStack gunStack, AttachmentType type) {
        String raw = gunStack.get(component(type));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(raw));
    }

    private static void removeDamage(ItemStack gunStack, AttachmentType type) {
        DataComponentType<Integer> damageComponent = damageComponent(type);
        if (damageComponent != null) {
            gunStack.remove(damageComponent);
        }
    }

    private static int ensureFlashlightBattery(ItemStack gunStack) {
        Integer stored = gunStack.get(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get());
        if (stored == null) {
            gunStack.set(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get(), FLASHLIGHT_MAX_BATTERY);
            return FLASHLIGHT_MAX_BATTERY;
        }
        int clamped = Math.clamp(stored, 0, FLASHLIGHT_MAX_BATTERY);
        if (clamped != stored) {
            gunStack.set(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get(), clamped);
        }
        return clamped;
    }

    private static void setFlashlightBattery(ItemStack gunStack, int battery) {
        gunStack.set(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get(), Math.clamp(battery, 0, FLASHLIGHT_MAX_BATTERY));
    }

    private static void removeFlashlightState(ItemStack gunStack) {
        gunStack.remove(ModDataComponents.GUN_FLASHLIGHT_POWERED.get());
        gunStack.remove(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get());
    }

    private static DataComponentType<String> component(AttachmentType type) {
        return switch (type) {
            case SCOPE -> ModDataComponents.GUN_SCOPE_ATTACHMENT.get();
            case BARREL -> ModDataComponents.GUN_BARREL_ATTACHMENT.get();
            case STOCK -> ModDataComponents.GUN_STOCK_ATTACHMENT.get();
            case UNDER_BARREL -> ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT.get();
            case MAGAZINE -> ModDataComponents.GUN_MAGAZINE_ATTACHMENT.get();
            case SPECIAL -> ModDataComponents.GUN_SPECIAL_ATTACHMENT.get();
            case PAINT_JOB -> ModDataComponents.GUN_PAINT_JOB_ATTACHMENT.get();
            case DYE -> ModDataComponents.GUN_DYE_ATTACHMENT.get();
            case KILL_EFFECT -> ModDataComponents.GUN_KILL_EFFECT_ATTACHMENT.get();
        };
    }

    private static DataComponentType<StoredAttachmentStack> stackComponent(AttachmentType type) {
        return switch (type) {
            case SCOPE -> ModDataComponents.GUN_SCOPE_ATTACHMENT_STACK.get();
            case BARREL -> ModDataComponents.GUN_BARREL_ATTACHMENT_STACK.get();
            case STOCK -> ModDataComponents.GUN_STOCK_ATTACHMENT_STACK.get();
            case UNDER_BARREL -> ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT_STACK.get();
            case MAGAZINE -> ModDataComponents.GUN_MAGAZINE_ATTACHMENT_STACK.get();
            case SPECIAL -> ModDataComponents.GUN_SPECIAL_ATTACHMENT_STACK.get();
            case PAINT_JOB -> ModDataComponents.GUN_PAINT_JOB_ATTACHMENT_STACK.get();
            case DYE -> ModDataComponents.GUN_DYE_ATTACHMENT_STACK.get();
            case KILL_EFFECT -> ModDataComponents.GUN_KILL_EFFECT_ATTACHMENT_STACK.get();
        };
    }

    private static DataComponentType<Integer> damageComponent(AttachmentType type) {
        return switch (type) {
            case SCOPE -> ModDataComponents.GUN_SCOPE_ATTACHMENT_DAMAGE.get();
            case BARREL -> ModDataComponents.GUN_BARREL_ATTACHMENT_DAMAGE.get();
            case STOCK -> ModDataComponents.GUN_STOCK_ATTACHMENT_DAMAGE.get();
            case UNDER_BARREL -> ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT_DAMAGE.get();
            default -> null;
        };
    }
}
