package ttv.migami.jeg.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ttv.migami.jeg.faction.patrol.GunnerPatrolSpawner;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;
import ttv.migami.jeg.faction.raid.FactionRaidManager;
import ttv.migami.jeg.faction.raid.GunnerRaidSpawner;
import ttv.migami.jeg.faction.raid.HomeRaidTriggerManager;

public final class FactionEventTicker {
    private static final GunnerPatrolSpawner PATROL_SPAWNER = new GunnerPatrolSpawner();
    private static final GunnerRaidSpawner RAID_SPAWNER = new GunnerRaidSpawner();
    private static int tickCounter;

    private FactionEventTicker() {}

    public static void reschedulePatrolSpawner() {
        PATROL_SPAWNER.reschedule();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld != null) {
            PATROL_SPAWNER.tick(overworld, true, false);
            RAID_SPAWNER.tick(overworld, true, false);
        }

        if (++tickCounter % 20 == 0) {
            PatrolEncounterManager.tickAll(event.getServer());
            FactionRaidManager.tickAll(event.getServer());
            HomeRaidTriggerManager.tick(event.getServer());
        }

        MainThreadLevelActionScheduler.tick(event.getServer());
    }
}
