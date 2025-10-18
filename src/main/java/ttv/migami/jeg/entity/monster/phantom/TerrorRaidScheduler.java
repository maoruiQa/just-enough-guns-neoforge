package ttv.migami.jeg.entity.monster.phantom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class TerrorRaidScheduler {
    private static final Map<ServerLevel, List<ScheduledTask>> TASKS = new ConcurrentHashMap<>();
    private static boolean registered;

    private TerrorRaidScheduler() {}

    static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        ensureRegistered();
        if (delayTicks <= 0) {
            action.run();
            return;
        }
        TASKS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new ScheduledTask(delayTicks, action));
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
            Iterator<ScheduledTask> taskIterator = tasks.iterator();
            while (taskIterator.hasNext()) {
                ScheduledTask task = taskIterator.next();
                task.ticks--;
                if (task.ticks <= 0) {
                    task.action.run();
                    taskIterator.remove();
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
    }
}
