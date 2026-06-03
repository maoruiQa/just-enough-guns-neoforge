package ttv.migami.jeg.client.screen;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.menu.AttachmentMenu;

public final class AttachmentScreen extends AbstractContainerScreen<AttachmentMenu> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/gui/attachments.png");

    public AttachmentScreen(AttachmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.fill(this.leftPos + 7, this.topPos + 21, this.leftPos + 169, this.topPos + 57, 0x66000000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        Slot slot = this.getSlotUnderMouse();
        int menuSlot = slot == null ? -1 : this.menu.slots.indexOf(slot);
        if (menuSlot >= 0 && menuSlot < AttachmentType.values().length && slot.getItem().isEmpty()) {
            AttachmentType type = this.menu.typeAt(menuSlot);
            Component name = Component.translatable("slot.jeg.attachment." + type.key());
            if (slot.isActive()) {
                guiGraphics.renderComponentTooltip(this.font, List.of(name), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(name, Component.translatable("slot.jeg.attachment.not_applicable").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            }
        }
    }
}
