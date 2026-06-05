package ttv.migami.jeg.item.attachment;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class StoredAttachmentStack {
    public static final Codec<StoredAttachmentStack> CODEC = ItemStack.CODEC.xmap(StoredAttachmentStack::new, StoredAttachmentStack::toItemStack);
    public static final StreamCodec<RegistryFriendlyByteBuf, StoredAttachmentStack> STREAM_CODEC = ItemStack.STREAM_CODEC.map(StoredAttachmentStack::new, StoredAttachmentStack::toItemStack);

    private final ItemStack stack;

    public StoredAttachmentStack(ItemStack stack) {
        this.stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public ItemStack toItemStack() {
        return this.stack.copy();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StoredAttachmentStack stored)) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(this.stack, stored.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStack.hashItemAndComponents(this.stack));
    }
}
