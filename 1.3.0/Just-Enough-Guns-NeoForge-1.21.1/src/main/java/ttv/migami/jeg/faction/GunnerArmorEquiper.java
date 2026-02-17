package ttv.migami.jeg.faction;

import net.minecraft.world.entity.PathfinderMob;

/**
 * DISABLED: Bulletproof armor system temporarily disabled for 1.21.1 port
 * due to missing Equipment API. This will be re-enabled once the API is available.
 */
public class GunnerArmorEquiper {
    // All methods stubbed out - armor system disabled

    public static void equipGunnerArmor(net.minecraft.util.RandomSource random, GunnerArmorContext context) {
        // No-op: armor system disabled
    }

    /**
     * Stub context class for armor equipment (disabled)
     */
    public static class GunnerArmorContext {
        private final PathfinderMob mob;

        private GunnerArmorContext(PathfinderMob mob) {
            this.mob = mob;
        }

        public static GunnerArmorContext normal(PathfinderMob mob) {
            return new GunnerArmorContext(mob);
        }

        public static GunnerArmorContext elite(PathfinderMob mob) {
            return new GunnerArmorContext(mob);
        }

        public static GunnerArmorContext special(PathfinderMob mob) {
            return new GunnerArmorContext(mob);
        }

        public PathfinderMob getMob() {
            return mob;
        }
    }
}
