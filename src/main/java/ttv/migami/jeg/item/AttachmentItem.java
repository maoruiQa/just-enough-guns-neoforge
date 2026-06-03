package ttv.migami.jeg.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachmentRules;

public class AttachmentItem extends Item {
    private final AttachmentType type;
    private final AttachmentModifiers modifiers;

    public AttachmentItem(AttachmentType type, AttachmentModifiers modifiers, Properties properties) {
        super(properties);
        this.type = type;
        this.modifiers = modifiers;
    }

    public AttachmentType type() {
        return this.type;
    }

    public AttachmentModifiers modifiers() {
        return this.modifiers;
    }

    public boolean canAttachTo(ItemStack gunStack) {
        return GunAttachmentRules.canAttach(gunStack, this.type);
    }
}
