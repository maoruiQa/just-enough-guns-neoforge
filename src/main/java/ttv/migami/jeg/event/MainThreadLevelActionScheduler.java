package ttv.migami.jeg.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainThreadLevelActionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MainThreadLevelActionScheduler");
    private static final Map<ServerLevel, Queue<ScheduledTask>> TASKS = new ConcurrentHashMap<>();

    private MainThreadLevelActionScheduler() {}

    public static void scheduleNextTick(ServerLevel level, Runnable action) {
        TASKS.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>())
                .add(new ScheduledTask(level.getGameTime() + 1L, action));
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<ServerLevel, Queue<ScheduledTask>> entry : TASKS.entrySet()) {
            ServerLevel level = entry.getKey();
            if (level.getServer() != server) {
                continue;
            }

            long currentTick = level.getGameTime();
            Queue<ScheduledTask> tasks = entry.getValue();
            List<ScheduledTask> deferred = new ArrayList<>();
            ScheduledTask task;
            while ((task = tasks.poll()) != null) {
                if (task.runAtTick > currentTick) {
                    deferred.add(task);
                    continue;
                }

                try {
                    task.action.run();
                } catch (Exception e) {
                    LOGGER.error("Failed to run deferred level action in {}", level.dimension(), e);
                }
            }

            for (ScheduledTask deferredTask : deferred) {
                tasks.add(deferredTask);
            }
        }
    }

    private static final class ScheduledTask {
        private final long runAtTick;
        private final Runnable action;

        private ScheduledTask(long runAtTick, Runnable action) {
            this.runAtTick = runAtTick;
            this.action = action;
        }
    }
}
