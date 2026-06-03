package ttv.migami.jeg.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.menu.AttachmentMenu;

public final class AttachmentScreen extends AbstractContainerScreen<AttachmentMenu> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/gui/attachments.png");
    private static final int SLOT_ICON_U = 176;
    private static final int DISABLED_SLOT_ICON_V = 0;
    private static final int SLOT_ICON_SIZE = 16;
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
            guiGraphics.drawCenteredString(this.font, Component.literal(Reference.MOD_ID), this.imageWidth / 2, -30, 0x686C71);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderAttachmentSlotTooltip(guiGraphics, mouseX, mouseY);
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
            guiGraphics.renderComponentTooltip(this.font, List.of(name), mouseX, mouseY);
        }
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
}
