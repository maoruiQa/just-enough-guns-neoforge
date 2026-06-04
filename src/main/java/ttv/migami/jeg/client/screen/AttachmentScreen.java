package ttv.migami.jeg.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.ClientUiConfig;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.network.NetworkHandler;

public final class AttachmentScreen extends AbstractContainerScreen<AttachmentMenu> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/gui/attachments.png");
    private static final int SLOT_ICON_U = 176;
    private static final int DISABLED_SLOT_ICON_V = 0;
    private static final int SLOT_ICON_SIZE = 16;
    private static final int CONFIG_BUTTON_Y = 90;
    private static final int CONFIG_BUTTON_U = 192;
    private static final int CONFIG_BUTTON_V = 0;
    private static final int CONFIG_BUTTON_SIZE = 10;
    private static final int MEDAL_BUTTON_X = -31;
    private static final int MEDAL_BUTTON_Y = 148;
    private static final int MEDAL_BUTTON_SIZE = 22;
    private static final int MEDAL_ENABLED_U = 176;
    private static final int MEDAL_ENABLED_V = 161;
    private static final int MEDAL_DISABLED_U = 176;
    private static final int MEDAL_DISABLED_V = 183;
    private static final int PREVIEW_LIGHT = LightTexture.FULL_BRIGHT;

    public AttachmentScreen(AttachmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player != null && !(player.getMainHandItem().getItem() instanceof GunItem)) {
            this.onClose();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos - 71, this.topPos - 18, 203, 0, 32, 202);
        this.renderConfigButton(guiGraphics, mouseX, mouseY);
        this.renderMedalButton(guiGraphics);
        this.renderGunPreview(guiGraphics, partialTick);
        this.renderAttachmentSlotIcons(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack gunStack = this.previewGunStack();
        if (gunStack.isEmpty()) {
            guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        } else {
            guiGraphics.drawCenteredString(this.font, gunStack.getHoverName(), this.imageWidth / 2, -42, gunStack.getRarity().color().getColor());
            guiGraphics.drawCenteredString(this.font, this.previewGunModName(gunStack), this.imageWidth / 2, -30, 0x686C71);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderAttachmentSlotTooltip(guiGraphics, mouseX, mouseY);
        this.renderConfigButtonTooltip(guiGraphics, mouseX, mouseY);
        this.renderMedalButtonTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && this.isMouseOverConfigButton((int) mouseX, (int) mouseY)) {
            this.openConfigScreen();
            return true;
        }
        if ((button == 0 || button == 1) && this.isMouseOverMedalButton((int) mouseX, (int) mouseY)) {
            if (ClientUiConfig.hideMedals()) {
                return true;
            }
            NetworkHandler.sendToggleMedals();
            Player player = this.minecraft == null ? null : this.minecraft.player;
            if (player != null) {
                var sound = ttv.migami.jeg.init.ModSounds.ALL.get(Reference.id("ui.medal.generic"));
                if (sound != null) {
                    player.playSound(sound.get(), 1.0F, 1.0F);
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderGunPreview(GuiGraphics guiGraphics, float partialTick) {
        ItemStack gunStack = this.previewGunStack();
        if (gunStack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.leftPos + this.imageWidth / 2.0D, this.topPos + 24.0D, 150.0D);
        poseStack.scale(75.0F, -75.0F, 75.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(player.tickCount + partialTick));
        minecraft.getItemRenderer().renderStatic(
                player,
                gunStack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                buffer,
                minecraft.level,
                PREVIEW_LIGHT,
                OverlayTexture.NO_OVERLAY,
                player.getId()
        );
        buffer.endBatch();
        poseStack.popPose();
    }

    private void renderConfigButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (Config.hideAttachmentConfigButton()) {
            return;
        }
        int x = this.configButtonX();
        int y = this.topPos + CONFIG_BUTTON_Y;
        guiGraphics.blit(TEXTURE, x, y, CONFIG_BUTTON_U, CONFIG_BUTTON_V, CONFIG_BUTTON_SIZE, CONFIG_BUTTON_SIZE);
        if (this.isMouseOverConfigButton(mouseX, mouseY)) {
            guiGraphics.fillGradient(x, y, x + CONFIG_BUTTON_SIZE, y + CONFIG_BUTTON_SIZE, 0x80FFFFFF, 0x80FFFFFF);
        }
    }

    private void renderConfigButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!Config.hideAttachmentConfigButton() && this.isMouseOverConfigButton(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("jeg.button.config.tooltip")), mouseX, mouseY);
        }
    }

    private boolean isMouseOverConfigButton(int mouseX, int mouseY) {
        if (Config.hideAttachmentConfigButton()) {
            return false;
        }
        int x = this.configButtonX();
        int y = this.topPos + CONFIG_BUTTON_Y;
        return mouseX >= x && mouseX < x + CONFIG_BUTTON_SIZE && mouseY >= y && mouseY < y + CONFIG_BUTTON_SIZE;
    }

    private int configButtonX() {
        if (Config.leftAttachmentButtons()) {
            return this.leftPos + this.font.width(this.title) + 11;
        }
        return this.leftPos + this.imageWidth - 17;
    }

    private void openConfigScreen() {
        ModList.get().getModContainerById(Reference.MOD_ID).ifPresent(container -> {
            Screen screen = container.getCustomExtension(IConfigScreenFactory.class)
                    .map(factory -> factory.createScreen(container, this))
                    .orElse(null);
            if (screen != null) {
                this.minecraft.setScreen(screen);
            } else if (this.minecraft != null && this.minecraft.player != null) {
                MutableComponent modName = Component.literal("Configured");
                modName.setStyle(modName.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("jeg.chat.open_curseforge_page")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/configured")));
                this.minecraft.player.displayClientMessage(Component.translatable("jeg.chat.install_configured", modName), false);
            }
        });
    }

    private void renderAttachmentSlotIcons(GuiGraphics guiGraphics) {
        AttachmentType[] types = AttachmentType.values();
        for (int index = 0; index < types.length; index++) {
            Slot slot = this.menu.getSlot(index);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            if (!this.canPlaceAttachmentInSlot(slot)) {
                guiGraphics.blit(TEXTURE, x, y, SLOT_ICON_U, DISABLED_SLOT_ICON_V, SLOT_ICON_SIZE, SLOT_ICON_SIZE);
            } else if (slot.getItem().isEmpty()) {
                guiGraphics.blit(TEXTURE, x, y, SLOT_ICON_U, 16 + index * SLOT_ICON_SIZE, SLOT_ICON_SIZE, SLOT_ICON_SIZE);
            }
        }
    }

    private void renderAttachmentSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        int menuSlot = attachmentSlotIndex(slot);
        if (menuSlot < 0) {
            return;
        }

        AttachmentType type = this.menu.typeAt(menuSlot);
        Component name = Component.translatable("slot.jeg.attachment." + type.key());
        if (!slot.isActive()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(name, Component.translatable("slot.jeg.attachment.not_applicable").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            return;
        }
        if (slot.getItem().isEmpty() && !this.isCarriedCompatible(slot)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("slot.jeg.attachment.incompatible").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            return;
        }
        if (slot.getItem().isEmpty()) {
            if (type == AttachmentType.BARREL) {
                guiGraphics.renderComponentTooltip(this.font, List.of(name, Component.translatable("slot.jeg.attachment.swords").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(name), mouseX, mouseY);
            }
        }
    }

    private void renderMedalButton(GuiGraphics guiGraphics) {
        boolean enabled = GunAttachments.areMedalsEnabled(this.previewGunStack());
        int u = enabled ? MEDAL_ENABLED_U : MEDAL_DISABLED_U;
        int v = enabled ? MEDAL_ENABLED_V : MEDAL_DISABLED_V;
        guiGraphics.blit(TEXTURE, this.leftPos + MEDAL_BUTTON_X, this.topPos + MEDAL_BUTTON_Y, u, v, MEDAL_BUTTON_SIZE, MEDAL_BUTTON_SIZE);
    }

    private void renderMedalButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.isMouseOverMedalButton(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(medalButtonTooltip()), mouseX, mouseY);
        }
    }

    private static Component medalButtonTooltip() {
        return ClientUiConfig.hideMedals()
                ? Component.translatable("slot.jeg.toggle_medals.disabled").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("slot.jeg.toggle_medals").withStyle(ChatFormatting.YELLOW);
    }

    private boolean isMouseOverMedalButton(int mouseX, int mouseY) {
        int x = this.leftPos + MEDAL_BUTTON_X;
        int y = this.topPos + MEDAL_BUTTON_Y;
        return mouseX >= x && mouseX < x + MEDAL_BUTTON_SIZE && mouseY >= y && mouseY < y + MEDAL_BUTTON_SIZE;
    }

    private boolean canPlaceAttachmentInSlot(Slot slot) {
        if (!slot.isActive()) {
            return false;
        }
        if (slot != this.getSlotUnderMouse() || !slot.getItem().isEmpty()) {
            return true;
        }
        return this.isCarriedCompatible(slot);
    }

    private boolean isCarriedCompatible(Slot slot) {
        ItemStack carried = this.menu.getCarried();
        return carried.isEmpty() || slot.mayPlace(carried);
    }

    private int attachmentSlotIndex(Slot slot) {
        if (slot == null) {
            return -1;
        }
        int index = this.menu.slots.indexOf(slot);
        return index >= 0 && index < AttachmentType.values().length ? index : -1;
    }

    private ItemStack previewGunStack() {
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof GunItem ? stack : ItemStack.EMPTY;
    }

    private Component previewGunModName(ItemStack gunStack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(gunStack.getItem());
        if (Reference.id("abstract_gun").equals(itemId)) {
            return Component.literal("JEG: Gun-Packs!");
        }
        String modName = ModList.get().getModContainerById(itemId.getNamespace())
                .map(container -> container.getModInfo().getDisplayName())
                .orElse("JEG: Add-on");
        return Component.literal(modName);
    }
}
