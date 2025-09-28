package ttv.migami.jeg.util;

import net.minecraft.nbt.Tag;

/**
 * Minimal replacement for the old Forge INBTSerializable interface.
 */
public interface INBTSerializable<T extends Tag> {
    T serializeNBT();

    void deserializeNBT(T tag);
}
