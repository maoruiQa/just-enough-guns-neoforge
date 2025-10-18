package ttv.migami.jeg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;

import java.util.function.Consumer;

public final class ArmoredJoyHarnessItem extends Item {
    public enum HarnessTier {
        BASE,
        DIAMOND,
        NETHERITE
    }

    @Nullable
    private final DyeColor color;
    @Nullable
    private final Component materialComponent;
    private final float initialPlating;
    private final float maxPlating;
    private final HarnessTier tier;

    public ArmoredJoyHarnessItem(@Nullable DyeColor color, Properties properties, float initialPlating, float maxPlating, @Nullable Component materialComponent) {
        this(color, properties, initialPlating, maxPlating, materialComponent, HarnessTier.BASE);
    }

    public ArmoredJoyHarnessItem(@Nullable DyeColor color, Properties properties, float initialPlating, float maxPlating, @Nullable Component materialComponent, HarnessTier tier) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .component(ModDataComponents.ARMORED_HARNESS_PLATING.get(), initialPlating));
        this.color = color;
        this.initialPlating = initialPlating;
        this.maxPlating = maxPlating;
        this.materialComponent = materialComponent;
        this.tier = tier;
    }

    public static Properties buildProperties(Properties base, @Nullable DyeColor color, HarnessTier tier) {
        var builder = Equippable.builder(EquipmentSlot.BODY)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_IRON)
                .setAllowedEntities(net.minecraft.world.entity.EntityType.HAPPY_GHAST)
                .setEquipOnInteract(true)
                .setDamageOnHurt(false);

        ResourceKey<EquipmentAsset> assetKey = resolveAsset(color, tier);
        if (assetKey != null) {
            builder.setAsset(assetKey);
        }

        Equippable equippable = builder.build();
        base = base.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, equippable);
        if (tier != HarnessTier.BASE) {
            base = base.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return base;
    }

    @Nullable
    private static ResourceKey<EquipmentAsset> resolveAsset(@Nullable DyeColor color, HarnessTier tier) {
        if (color == null) {
            return null;
        }
        String suffix = switch (tier) {
            case BASE -> "";
            case DIAMOND -> "_diamond";
            case NETHERITE -> "_netherite";
        };
        String name = color.getName() + suffix + "_harness";
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Reference.id(name));
    }

    @Nullable
    public DyeColor getColor() {
        return this.color;
    }

    public float getMaxPlating() {
        return this.maxPlating;
    }

    public float getInitialPlating() {
        return this.initialPlating;
    }

    public HarnessTier tier() {
        return this.tier;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(@NotNull ItemStack stack) {
        Component base = super.getName(stack);
        if (this.tier != HarnessTier.BASE) {
            return base.copy().withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        return base;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        if (this.color != null) {
            tooltipAdder.accept(Component.translatable("tooltip.jeg.harness_color",
                    Component.translatable("color.minecraft." + this.color.getName()))
                    .withStyle(ChatFormatting.GRAY));
        } else if (this.materialComponent != null) {
            tooltipAdder.accept(Component.translatable("tooltip.jeg.harness_material", this.materialComponent.copy())
                    .withStyle(ChatFormatting.GRAY));
        }

        float plating = HappyGhastArmorHelper.getPlating(stack);
        tooltipAdder.accept(Component.translatable("tooltip.jeg.harness_plating",
                String.format("%.0f", plating), String.format("%.0f", this.maxPlating))
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull net.minecraft.world.entity.LivingEntity target, @NotNull InteractionHand hand) {
        if (!(target instanceof HappyGhast ghast) || ghast.isBaby()) {
            return InteractionResult.PASS;
        }

        Level level = player.level();
        ItemStack current = ghast.getItemBySlot(EquipmentSlot.BODY);

        if (!level.isClientSide()) {
            ItemStack equipped = stack.copyWithCount(1);
            HappyGhastArmorHelper.setPlating(equipped, this.initialPlating);
            Vec3 pos = ghast.position();

            if (!current.isEmpty()) {
                ghast.setBodyArmorItem(equipped);
                HappyGhastArmorHelper.syncAbsorption(ghast);
                HappyGhastArmorEvents.notifyPassengers(ghast);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.NEUTRAL, 1.0F, 1.15F);

                ItemStack returned = current.copy();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (!player.getAbilities().instabuild && stack.isEmpty()) {
                    player.setItemInHand(hand, returned);
                } else if (!player.addItem(returned)) {
                    player.drop(returned, false);
                }
                return InteractionResult.SUCCESS_SERVER;
            }

            ghast.setBodyArmorItem(equipped);
            HappyGhastArmorHelper.syncAbsorption(ghast);
            HappyGhastArmorEvents.notifyPassengers(ghast);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.NEUTRAL, 1.0F, 1.15F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }
}
