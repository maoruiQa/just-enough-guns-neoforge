package ttv.migami.jeg.gun;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import ttv.migami.jeg.Reference;

public final class MagazineModeData extends SavedData {
    public static final Codec<MagazineModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("magazineFeed", true).forGetter(data -> data.magazineFeed),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(data -> data.revision),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(data -> data.initialized),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("confirmed", Map.of()).forGetter(data -> data.confirmed)
    ).apply(instance, MagazineModeData::new));
    private static final SavedDataType<MagazineModeData> TYPE = new SavedDataType<>(
            Reference.id("magazine_mode"), MagazineModeData::new, CODEC, null);

    public MagazineModeData() {}

    private MagazineModeData(boolean mode, long revision, boolean initialized, Map<String, Long> confirmed) {
        this.magazineFeed = mode;
        this.revision = Math.max(0, revision);
        this.initialized = initialized || revision != 0;
        this.confirmed.putAll(confirmed);
    }

    public static MagazineModeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
