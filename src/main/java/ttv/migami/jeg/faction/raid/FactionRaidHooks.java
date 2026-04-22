package ttv.migami.jeg.faction.raid;

import net.minecraft.world.entity.PathfinderMob;

public final class FactionRaidHooks {
    private FactionRaidHooks() {}

    public static void recoverRaidMob(PathfinderMob mob) {
        FactionRaidManager.recoverRaidMob(mob);
    }

    public static void recoverRaidAnchor(RaidEntity anchor) {
        FactionRaidManager.recoverRaidAnchor(anchor);
    }
}
