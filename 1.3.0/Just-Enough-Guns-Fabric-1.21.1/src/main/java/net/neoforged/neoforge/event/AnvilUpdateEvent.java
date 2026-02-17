package net.neoforged.neoforge.event;

import net.minecraft.world.item.ItemStack;

public class AnvilUpdateEvent {
    private final ItemStack left;
    private final ItemStack right;
    private ItemStack output = ItemStack.EMPTY;
    private boolean canceled;

    public AnvilUpdateEvent(ItemStack left, ItemStack right) {
        this.left = left;
        this.right = right;
    }

    public ItemStack getLeft() {
        return left;
    }

    public ItemStack getRight() {
        return right;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
