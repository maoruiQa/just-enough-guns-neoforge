package ttv.migami.jeg.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.ServerAdvancementManager;
import ttv.migami.jeg.Reference;

/** One-time-equivalent, idempotent import into vanilla's existing advancement store. */
public final class AdvancementMigration {
    private static final JsonObject RULES = readRules();

    private AdvancementMigration() {}

    private static JsonObject readRules() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(AdvancementMigration.class
                .getResourceAsStream("/data/jeg/advancement_migration.json")), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read bundled advancement migration", exception);
        }
    }

    public static Set<AdvancementHolder> apply(ServerAdvancementManager manager,
            Map<AdvancementHolder, AdvancementProgress> progress) {
        Set<AdvancementHolder> changed = new HashSet<>();
        for (var target : RULES.getAsJsonObject("criteria").entrySet()) {
            AdvancementHolder holder = holder(manager, target.getKey());
            if (holder == null) continue;
            for (var criterion : target.getValue().getAsJsonObject().entrySet()) {
                for (JsonElement source : criterion.getValue().getAsJsonArray()) {
                    JsonArray pair = source.getAsJsonArray();
                    AdvancementProgress old = progress.get(holder(manager, pair.get(0).getAsString()));
                    var obtained = old == null ? null : old.getCriterion(pair.get(1).getAsString());
                    if (obtained != null && obtained.isDone()) {
                        grant(progress, holder, criterion.getKey(), changed);
                        break;
                    }
                }
            }
        }
        // The generated order contains four chapter goals followed by the career goal.
        for (var goal : RULES.getAsJsonObject("completion").entrySet()) {
            AdvancementHolder holder = holder(manager, goal.getKey());
            if (holder == null) continue;
            boolean complete = true;
            for (JsonElement member : goal.getValue().getAsJsonArray()) {
                AdvancementProgress saved = progress.get(holder(manager, member.getAsString()));
                if (saved == null || !saved.isDone()) { complete = false; break; }
            }
            if (complete) {
                for (String criterion : holder.value().criteria().keySet()) grant(progress, holder, criterion, changed);
            }
        }
        return changed;
    }

    private static AdvancementHolder holder(ServerAdvancementManager manager, String id) {
        return id.startsWith("jeg:") ? manager.get(Reference.id(id.substring(4))) : null;
    }

    private static void grant(Map<AdvancementHolder, AdvancementProgress> progress,
            AdvancementHolder holder, String criterion, Set<AdvancementHolder> changed) {
        AdvancementProgress saved = progress.computeIfAbsent(holder, key -> {
            AdvancementProgress created = new AdvancementProgress();
            created.update(key.value().requirements());
            return created;
        });
        // Bypass award(): imported history must not run rewards, announcements or earned events.
        if (saved.grantProgress(criterion)) changed.add(holder);
    }
}
