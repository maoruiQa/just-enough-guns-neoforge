package ttv.migami.jeg.entity.monster.phantom;

import net.minecraft.world.entity.PathfinderMob;

public final class TerrorRaidHooks {
    private TerrorRaidHooks() {}

    public static void recoverRaidMob(PathfinderMob mob) {
        TerrorRaidManager.recoverRaidMob(mob);
    }
}
