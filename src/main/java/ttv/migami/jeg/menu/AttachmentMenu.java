package ttv.migami.jeg.menu;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.AttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachmentRules;
import ttv.migami.jeg.item.attachment.GunAttachments;

public final class AttachmentMenu extends AbstractContainerMenu {
    private static final AttachmentType[] TYPES = AttachmentType.values();
    private static final int ATTACHMENT_SLOT_COUNT = TYPES.length;
    private static final int PLAYER_INVENTORY_START = ATTACHMENT_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ItemStack weapon;
    private final int selectedSlot;
    private final Container attachments = new SimpleContainer(ATTACHMENT_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            AttachmentMenu.this.writeAttachments();
        }
    };
    private boolean loaded;

    public AttachmentMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ATTACHMENTS.get(), containerId);
        this.weapon = playerInventory.getSelected();
        this.selectedSlot = playerInventory.selected;
        this.loadAttachments();

        for (int index = 0; index < ATTACHMENT_SLOT_COUNT; index++) {
            this.addSlot(new AttachmentSlot(this.attachments, this.weapon, TYPES[index], playerInventory.player, index, 17 + index * 24, 32));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            Slot slot = col == this.selectedSlot
                    ? new LockedSlot(playerInventory, col, 8 + col * 18, 142)
                    : new Slot(playerInventory, col, 8 + col * 18, 142);
            this.addSlot(slot);
        }
        this.loaded = true;
    }

    public static SimpleMenuProvider provider() {
        return new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new AttachmentMenu(containerId, playerInventory),
                net.minecraft.network.chat.Component.translatable("container.jeg.attachments")
        );
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public AttachmentType typeAt(int index) {
        return TYPES[index];
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.getInventory().getSelected() == this.weapon && this.weapon.getItem() instanceof GunItem;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < ATTACHMENT_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, ATTACHMENT_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    private void loadAttachments() {
        for (int index = 0; index < TYPES.length; index++) {
            AttachmentType type = TYPES[index];
            ItemStack stack = GunAttachments.item(this.weapon, type)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            this.attachments.setItem(index, stack);
        }
    }

    private void writeAttachments() {
        if (!this.loaded) {
            return;
        }
        for (int index = 0; index < TYPES.length; index++) {
            AttachmentType type = TYPES[index];
            ItemStack stack = this.attachments.getItem(index);
            if (stack.isEmpty()) {
                GunAttachments.clear(this.weapon, type);
            } else {
                GunAttachments.set(this.weapon, type, stack);
            }
        }
    }

    private static final class AttachmentSlot extends Slot {
        private final ItemStack weapon;
        private final AttachmentType type;
        private final Player player;

        private AttachmentSlot(Container container, ItemStack weapon, AttachmentType type, Player player, int index, int x, int y) {
            super(container, index, x, y);
            this.weapon = weapon;
            this.type = type;
            this.player = player;
        }

        @Override
        public boolean isActive() {
            return GunAttachmentRules.canAttach(this.weapon, this.type);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof AttachmentItem attachment
                    && attachment.type() == this.type
                    && attachment.canAttachTo(this.weapon);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!this.player.level().isClientSide()) {
                var holder = ModSounds.ALL.get(Reference.id("ui.weapon.attach"));
                if (holder != null) {
                    this.player.level().playSound(null, this.player.getX(), this.player.getY() + 1.0D, this.player.getZ(), holder.get(), SoundSource.PLAYERS, 0.5F, this.hasItem() ? 1.0F : 0.75F);
                }
            }
        }
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }
    }
}
