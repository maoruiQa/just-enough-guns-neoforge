package ttv.migami.jeg.init;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, Reference.MOD_ID);
    public static final Map<ResourceLocation, DeferredHolder<SoundEvent, SoundEvent>> ALL = new LinkedHashMap<>();

    static {
        for (ResourceLocation id : loadSoundKeys()) {
            register(id);
        }
    }

    /** Fixed hear distance (blocks) for sounds that must not scale as "variable range". */
    private static final Map<String, Float> FIXED_RANGE_BLOCKS = Map.of(
            // Gun fire volume 7.5 ≈ 120-block effective; C4 beep/final at 50%
            "item.c4.beep", 60.0F,
            "item.c4.final", 60.0F
    );

    private static void register(ResourceLocation id) {
        ALL.computeIfAbsent(id, key -> REGISTER.register(key.getPath(), () -> {
            Float fixed = FIXED_RANGE_BLOCKS.get(key.getPath());
            if (fixed != null) {
                return SoundEvent.createFixedRangeEvent(key, fixed);
            }
            return SoundEvent.createVariableRangeEvent(key);
        }));
    }

    private static List<ResourceLocation> loadSoundKeys() {
        JsonObject json = readSoundsJson();
        return json.keySet().stream().map(Reference::id).toList();
    }

    private static JsonObject readSoundsJson() {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sounds.json");
        String resource = "/assets/" + path.getNamespace() + "/" + path.getPath();
        try (InputStream stream = ModSounds.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing sounds.json at " + resource);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read sounds.json", e);
        }
    }
}
