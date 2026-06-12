package ttv.migami.jeg.client.screen;

import java.net.URI;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.ClientUiConfig;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.mixin.GuiGraphicsExtractorAccessor;
import ttv.migami.jeg.network.ClientNetworkHandler;

public final class AttachmentScreen extends AbstractContainerScreen<AttachmentMenu> {
    private static final Identifier TEXTURE = Reference.id("textures/gui/attachments.png");
    private static final int ATTACHMENT_PANEL_X = -71;
    private static final int ATTACHMENT_PANEL_Y = -18;
    private static final int ATTACHMENT_PANEL_WIDTH = 32;
    private static final int ATTACHMENT_PANEL_HEIGHT = 202;
    private static final int SLOT_ICON_U = 176;
    private static final int DISABLED_SLOT_ICON_V = 0;
    private static final int SLOT_ICON_SIZE = 16;
    private static final int CONFIG_BUTTON_Y = 90;
    private static final int CONFIG_BUTTON_U = 192;
    private static final int CONFIG_BUTTON_V = 0;
    private static final int CONFIG_BUTTON_SIZE = 10;
    private static final int THANKS_BUTTON_X = -67;
    private static final int THANKS_BUTTON_Y = 100;
    private static final int THANKS_BUTTON_SIZE = 22;
    private static final int MEDAL_BUTTON_X = -31;
    private static final int MEDAL_BUTTON_Y = 148;
    private static final int MEDAL_BUTTON_SIZE = 22;
    private static final int MEDAL_ENABLED_U = 176;
    private static final int MEDAL_ENABLED_V = 161;
    private static final int MEDAL_DISABLED_U = 176;
    private static final int MEDAL_DISABLED_V = 183;
    private static final float GUN_PREVIEW_HALF_EXTENT = 1.35F;
    private static final float GUN_PREVIEW_SCALE = 3.0F;
    private static final int GUN_PREVIEW_CENTER_Y = 42;
    private static final int GUN_PREVIEW_SLOT_CENTER_OFFSET = 8;
    private static final Vector3fc[] GUN_PREVIEW_EXTENTS = new Vector3fc[] {
            new Vector3f(-GUN_PREVIEW_HALF_EXTENT, -GUN_PREVIEW_HALF_EXTENT, -GUN_PREVIEW_HALF_EXTENT),
            new Vector3f(GUN_PREVIEW_HALF_EXTENT, GUN_PREVIEW_HALF_EXTENT, GUN_PREVIEW_HALF_EXTENT)
    };

    private static final class PreviewStateAccess {
        private static final Field LAYERS;
        private static final Field ACTIVE_LAYER_COUNT;

        static {
            try {
                LAYERS = ItemStackRenderState.class.getDeclaredField("layers");
                LAYERS.setAccessible(true);
                ACTIVE_LAYER_COUNT = ItemStackRenderState.class.getDeclaredField("activeLayerCount");
                ACTIVE_LAYER_COUNT.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to init item preview render state reflection access", e);
            }
        }

        private PreviewStateAccess() {}
    }

    public AttachmentScreen(AttachmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 184);
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
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos - 71, this.topPos - 18, 203, 0, 32, 202, 256, 256);
        this.renderConfigButton(guiGraphics, mouseX, mouseY);
        this.renderMedalButton(guiGraphics);
        this.renderGunPreview(guiGraphics);
        this.renderAttachmentSlotIcons(guiGraphics);
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        ItemStack gunStack = this.previewGunStack();
        if (gunStack.isEmpty()) {
            guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        } else {
            guiGraphics.centeredText(this.font, gunStack.getHoverName(), this.imageWidth / 2, -42, gunStack.getRarity().color().getColor());
            guiGraphics.centeredText(this.font, this.previewGunModName(gunStack), this.imageWidth / 2, -30, 0xFF686C71);
        }
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        this.extractAttachmentSlotTooltip(guiGraphics, mouseX, mouseY);
        this.extractConfigButtonTooltip(guiGraphics, mouseX, mouseY);
        this.extractThanksTooltip(guiGraphics, mouseX, mouseY);
        this.extractMedalButtonTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if ((button == 0 || button == 1) && this.isMouseOverConfigButton(mouseX, mouseY)) {
            this.openConfigScreen();
            return true;
        }
        if ((button == 0 || button == 1) && this.isMouseOverMedalButton(mouseX, mouseY)) {
            if (ClientUiConfig.hideMedals()) {
                return true;
            }
            ClientNetworkHandler.sendToggleMedals();
            Player player = this.minecraft == null ? null : this.minecraft.player;
            if (player != null) {
                var sound = ttv.migami.jeg.init.ModSounds.ALL.get(Reference.id("ui.medal.generic"));
                if (sound != null) {
                    player.playSound(sound.get(), 1.0F, 1.0F);
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top)
                && !this.isMouseOverAttachmentPanel(mouseX, mouseY, left, top);
    }

    private void renderGunPreview(GuiGraphicsExtractor guiGraphics) {
        ItemStack gunStack = this.previewGunStack();
        if (gunStack.isEmpty()) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        TrackingItemStackRenderState itemState = new TrackingItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(
                itemState,
                gunStack,
                ItemDisplayContext.FIXED,
                player
        );
        applyPreviewStateTransform(itemState);
        itemState.setOversizedInGui(true);

        GuiItemRenderState guiItemState = new GuiItemRenderState(
                new Matrix3x2f(guiGraphics.pose()),
                itemState,
                this.leftPos + this.imageWidth / 2 - GUN_PREVIEW_SLOT_CENTER_OFFSET,
                this.topPos + GUN_PREVIEW_CENTER_Y - GUN_PREVIEW_SLOT_CENTER_OFFSET,
                null
        );
        ((GuiGraphicsExtractorAccessor) guiGraphics).jeg$getGuiRenderState().addItem(guiItemState);
    }

    private void renderConfigButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (Config.hideAttachmentConfigButton()) {
            return;
        }
        int x = this.configButtonX();
        int y = this.topPos + CONFIG_BUTTON_Y;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, CONFIG_BUTTON_U, CONFIG_BUTTON_V, CONFIG_BUTTON_SIZE, CONFIG_BUTTON_SIZE, 256, 256);
        if (this.isMouseOverConfigButton(mouseX, mouseY)) {
            guiGraphics.fill(x, y, x + CONFIG_BUTTON_SIZE, y + CONFIG_BUTTON_SIZE, 0x80FFFFFF);
        }
    }

    private void extractConfigButtonTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!Config.hideAttachmentConfigButton() && this.isMouseOverConfigButton(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("jeg.button.config.tooltip")), mouseX, mouseY);
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

    private boolean isMouseOverAttachmentPanel(double mouseX, double mouseY, int left, int top) {
        int x = left + ATTACHMENT_PANEL_X;
        int y = top + ATTACHMENT_PANEL_Y;
        return mouseX >= x && mouseX < x + ATTACHMENT_PANEL_WIDTH && mouseY >= y && mouseY < y + ATTACHMENT_PANEL_HEIGHT;
    }

    private void extractThanksTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (this.isMouseOverThanksButton(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(
                    this.font,
                    List.of(Component.translatable("cutesy.jeg.thanks"), Component.literal("- MigaMi")),
                    mouseX,
                    mouseY
            );
        }
    }

    private boolean isMouseOverThanksButton(int mouseX, int mouseY) {
        int x = this.leftPos + THANKS_BUTTON_X;
        int y = this.topPos + THANKS_BUTTON_Y;
        return mouseX >= x && mouseX < x + THANKS_BUTTON_SIZE && mouseY >= y && mouseY < y + THANKS_BUTTON_SIZE;
    }

    private void openConfigScreen() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        MutableComponent modName = Component.literal("Configured");
        modName.setStyle(modName.getStyle()
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true)
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("jeg.chat.open_curseforge_page")))
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://www.curseforge.com/minecraft/mc-mods/configured"))));
        this.minecraft.player.sendSystemMessage(Component.translatable("jeg.chat.install_configured", modName));
    }

    private void renderAttachmentSlotIcons(GuiGraphicsExtractor guiGraphics) {
        AttachmentType[] types = AttachmentType.values();
        for (int index = 0; index < types.length; index++) {
            Slot slot = this.menu.getSlot(index);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            if (!this.canPlaceAttachmentInSlot(slot)) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, SLOT_ICON_U, DISABLED_SLOT_ICON_V, SLOT_ICON_SIZE, SLOT_ICON_SIZE, 256, 256);
            } else if (slot.getItem().isEmpty()) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, SLOT_ICON_U, 16 + index * SLOT_ICON_SIZE, SLOT_ICON_SIZE, SLOT_ICON_SIZE, 256, 256);
            }
        }
    }

    private void extractAttachmentSlotTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Slot slot = this.hoveredSlot;
        int menuSlot = attachmentSlotIndex(slot);
        if (menuSlot < 0) {
            return;
        }

        AttachmentType type = this.menu.typeAt(menuSlot);
        Component name = Component.translatable("slot.jeg.attachment." + type.key());
        if (!slot.isActive()) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(name, Component.translatable("slot.jeg.attachment.not_applicable").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            return;
        }
        if (slot.getItem().isEmpty() && !this.isCarriedCompatible(slot)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("slot.jeg.attachment.incompatible").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            return;
        }
        if (slot.getItem().isEmpty()) {
            if (type == AttachmentType.BARREL) {
                guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(name, Component.translatable("slot.jeg.attachment.swords").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            } else {
                guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(name), mouseX, mouseY);
            }
        }
    }

    private void renderMedalButton(GuiGraphicsExtractor guiGraphics) {
        boolean enabled = GunAttachments.areMedalsEnabled(this.previewGunStack());
        int u = enabled ? MEDAL_ENABLED_U : MEDAL_DISABLED_U;
        int v = enabled ? MEDAL_ENABLED_V : MEDAL_DISABLED_V;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + MEDAL_BUTTON_X, this.topPos + MEDAL_BUTTON_Y, u, v, MEDAL_BUTTON_SIZE, MEDAL_BUTTON_SIZE, 256, 256);
    }

    private void extractMedalButtonTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (this.isMouseOverMedalButton(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, List.of(medalButtonTooltip()), mouseX, mouseY);
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
        if (slot != this.hoveredSlot || !slot.getItem().isEmpty()) {
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
        Identifier itemId = BuiltInRegistries.ITEM.getKey(gunStack.getItem());
        if (Reference.id("abstract_gun").equals(itemId)) {
            return Component.literal("JEG: Gun-Packs!");
        }
        return Component.literal(Reference.MOD_ID.equals(itemId.getNamespace()) ? "JEGN" : "JEG: Add-on");
    }

    private static void applyPreviewStateTransform(ItemStackRenderState itemState) {
        try {
            ItemStackRenderState.LayerRenderState[] layers =
                    (ItemStackRenderState.LayerRenderState[]) PreviewStateAccess.LAYERS.get(itemState);
            int count = (int) PreviewStateAccess.ACTIVE_LAYER_COUNT.get(itemState);
            Matrix4f previewScale = new Matrix4f().scale(GUN_PREVIEW_SCALE);
            for (int i = 0; i < count && i < layers.length; i++) {
                layers[i].setExtents(() -> GUN_PREVIEW_EXTENTS);
                layers[i].setLocalTransform(previewScale);
            }
        } catch (IllegalAccessException ignored) {
        }
    }
}
