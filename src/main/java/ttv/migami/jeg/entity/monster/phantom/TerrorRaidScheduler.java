package ttv.migami.jeg.entity.monster.phantom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TerrorRaidScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerrorRaidScheduler");
    private static final Map<ServerLevel, List<ScheduledTask>> TASKS = new ConcurrentHashMap<>();
    private static boolean registered;

    private TerrorRaidScheduler() {}

    static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        ensureRegistered();

        if (delayTicks <= 0) {
            action.run();
            return;
        }

        List<ScheduledTask> levelTasks = TASKS.computeIfAbsent(level, ignored -> new CopyOnWriteArrayList<>());
        levelTasks.add(new ScheduledTask(delayTicks, action));
    }

    private static void ensureRegistered() {
        if (!registered) {
            NeoForge.EVENT_BUS.addListener(TerrorRaidScheduler::onServerTick);
            registered = true;
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<ServerLevel, List<ScheduledTask>>> iterator = TASKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerLevel, List<ScheduledTask>> entry = iterator.next();
            ServerLevel level = entry.getKey();
            if (level.getServer() != event.getServer()) {
                continue;
            }

            List<ScheduledTask> tasks = entry.getValue();
            for (ScheduledTask task : tasks) {
                task.ticks--;
                if (task.ticks <= 0) {
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        // Log error silently
                    }
                    tasks.remove(task);
                }
            }
            if (tasks.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static final class ScheduledTask {
        private int ticks;
        private final Runnable action;

        private ScheduledTask(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }

        @Override
        public String toString() {
            return "ScheduledTask{ticks=" + ticks + ", action=" + action.getClass().getSimpleName() + "}";
        }
    }
}
