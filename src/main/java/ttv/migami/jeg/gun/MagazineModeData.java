package ttv.migami.jeg.gun;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.saveddata.SavedData;

public final class MagazineModeData extends SavedData {
    public static MagazineModeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MagazineModeData::new, MagazineModeData::load, null), "jeg_magazine_mode");
    }

    private static MagazineModeData load(CompoundTag tag, HolderLookup.Provider registries) {
        MagazineModeData data = new MagazineModeData();
        data.magazineFeed = tag.getBoolean("magazineFeed");
        data.revision = Math.max(0, tag.getLong("revision"));
        data.initialized = tag.contains("initialized") ? tag.getBoolean("initialized") : tag.contains("magazineFeed");
        CompoundTag confirmed = tag.getCompound("confirmed");
        for (String id : confirmed.getAllKeys()) data.confirmed.put(id, confirmed.getLong(id));
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("magazineFeed", magazineFeed);
        tag.putLong("revision", revision);
        tag.putBoolean("initialized", initialized);
        CompoundTag records = new CompoundTag();
        confirmed.forEach(records::putLong);
        tag.put("confirmed", records);
        return tag;
    }

    boolean magazineFeed = true;
    boolean initialized;
    long revision;
    final Map<String, Long> confirmed = new HashMap<>();
    final Map<UUID, Long> pending = new HashMap<>();
    RecipeManager appliedRecipes;
    long appliedGeneration = -1;

    boolean update(boolean mode) {
        if (!initialized) {
            initialized = true;
            magazineFeed = mode;
            setDirty();
            return false;
        }
        if (magazineFeed == mode) return false;
        magazineFeed = mode;
        revision++;
        pending.clear();
        setDirty();
        return true;
    }

}
