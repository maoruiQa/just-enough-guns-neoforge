package ttv.migami.jeg.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.raid.RaidDifficulty;
import ttv.migami.jeg.item.GunItem;

/** Natural gunners adapt locally; raid snapshots and vehicle crews retain their own rules. */
public final class NaturalGunnerDifficulty {
    public static final String NATURAL_TAG = "JEGNaturalGunner";
    private static final String PROFILE_TAG = "JEGNaturalDifficulty:";

    public record Profile(double score, int weaponTier, int armorTier) {
        String save() { return score + "," + weaponTier + "," + armorTier; }
    }

    private NaturalGunnerDifficulty() {}

    public static boolean applies(Mob mob) {
        return !mob.level().isClientSide() && Config.naturalGunnerDynamicDifficultyEnabled()
                && mob.entityTags().contains(NATURAL_TAG)
                && !mob.entityTags().contains(FactionSpawnHelper.RAID_TAG)
                && !mob.entityTags().contains(FactionSpawnHelper.PATROL_TAG)
                && !mob.entityTags().contains("TerrorRaidMob")
                && !mob.entityTags().contains(ttv.migami.jeg.vehicle.ai.EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG)
                && RaidDifficulty.forMob(mob) == null;
    }

    public static Profile profile(Mob mob) {
        for (String tag : mob.entityTags()) {
            if (tag.startsWith(PROFILE_TAG)) return load(tag.substring(PROFILE_TAG.length()));
        }
        return null;
    }

    public static Profile load(String value) {
        try {
            String[] parts = value.split(",", -1);
            if (parts.length != 3) return null;
            Profile p = new Profile(Double.parseDouble(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            return Double.isFinite(p.score) && p.score >= 0 && p.score <= 100
                    && p.weaponTier >= 0 && p.weaponTier <= 3 && p.armorTier >= 1 && p.armorTier <= 6 ? p : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    public static Profile refresh(PathfinderMob mob) {
        double equipment = 0;
        for (Player player : mob.level().players()) {
            if (player.isAlive() && player instanceof ServerPlayer serverPlayer
                    && serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && player.distanceToSqr(mob) <= 64 * 64) {
                equipment = Math.max(equipment, RaidDifficulty.equipmentScore(player));
            }
        }
        double score = score(equipment, Config.gunnerProgressionScale(mob.level()));
        var quality = RaidDifficulty.forScore(score, 0, 1);
        String type = GunnerType.keyFor(mob);
        Profile old = profile(mob);
        // Preserve existing time-based equipment growth and never downgrade a living gunner's kit.
        Profile next = new Profile(score,
                Math.max(Math.max(quality.weaponTier(), Config.gunnerWeaponMaxTier(mob.level(), type)), old == null ? 0 : old.weaponTier),
                Math.max(Math.max(quality.armorTier(), Config.gunnerArmorMaxTier(mob.level(), type)), old == null ? 1 : old.armorTier));
        mob.entityTags().stream().filter(tag -> tag.startsWith(PROFILE_TAG)).toList().forEach(mob::removeTag);
        mob.addTag(PROFILE_TAG + next.save());
        return next;
    }

    public static void tick(PathfinderMob mob) {
        if (!applies(mob) || mob.tickCount % 100 != 0) return;
        Profile old = profile(mob);
        Profile next = refresh(mob);
        if (BomberGunnerHelper.isBomber(mob)) return;
        if ((old == null || next.weaponTier > old.weaponTier) && mob.getMainHandItem().getItem() instanceof GunItem gun
                && !gun.getStats().id().getPath().equals("rocket_launcher")
                && GunnerProgression.weaponTier(gun) < next.weaponTier) {
            Faction faction = GunnerManager.getInstance().getFactionForMob(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
            if (faction != null) {
                var replacement = faction.getNaturalGun(mob.getRandom().nextBoolean(), mob.entityTags().contains("EliteGunner"), mob, true);
                if (replacement != null && GunnerProgression.weaponTier(replacement) > GunnerProgression.weaponTier(gun)) {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, GunnerMobSpawner.createModifiedGun(mob, replacement));
                    GunnerMobSpawner.suppressSkeletonSniperDrop(mob, replacement);
                }
            }
        }
        if (old == null || next.armorTier > old.armorTier) {
            GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), mob.entityTags().contains("EliteGunner")
                    ? GunnerArmorEquiper.GunnerArmorContext.elite(mob) : GunnerArmorEquiper.GunnerArmorContext.normal(mob));
        }
    }

    public static double score(double equipment, double timeProgress) {
        return Math.min(100, Mth.clamp(equipment, 0, 100) + 15 * Mth.clamp(timeProgress, 0, 1));
    }

    public static double eliteChance(double score) {
        return .4 * Mth.clamp(score, 0, 100) / 100;
    }

    public static double accuracyFloor(long day, int startDay, int maxDay) {
        double progress = Mth.clamp((day - (double) startDay) / Math.max(1, maxDay - startDay), 0, 1);
        return Math.floor(progress * 4) / 4;
    }

    /** Degrees, not hit percentage: cap the real projectile cone as well as scaling weapon spread. */
    public static float spread(float baseline, double score, long day, int startDay, int maxDay, double maxAccuracy) {
        double strength = Math.max(Mth.clamp(score, 0, 100) / 100, accuracyFloor(day, startDay, maxDay));
        return (float) Math.max(0, Math.min(baseline * (1 - Mth.clamp(maxAccuracy, 0, .95) * strength), 6 - 4 * strength));
    }

    public static float spread(Mob mob, float baseline) {
        Profile p = profile(mob);
        return spread(baseline, p == null ? 0 : p.score, Config.currentGunnerDay(mob.level()),
                Config.gunnerAccuracyStartDay(), Config.gunnerAccuracyMaxDay(), Config.gunnerAccuracyMaxPercent());
    }

    public static void selfCheck() {
        assert score(0, 999) == 15 && score(100, 1) == 100 && score(-1, -1) == 0;
        assert eliteChance(0) == 0 && Math.abs(eliteChance(15) - .06) < 1e-9 && eliteChance(100) == .4;
        assert load("NaN,3,6") == null && load("100,4,6") == null && load("broken") == null;
        Profile p = new Profile(87.5, 3, 6);
        assert p.equals(load(p.save()));
        int[] days = {0, 18, 19, 32, 33, 46, 47, 59, 60, 10000};
        float[] ceilings = {6, 6, 5, 5, 4, 4, 3, 3, 2, 2};
        for (int i = 0; i < days.length; i++) {
            assert spread(100, 0, days[i], 5, 60, .7) == ceilings[i];
            assert spread(100, 100, days[i], 5, 60, .7) == 2;
        }
        assert spread(100, 50, 0, 5, 60, .7) == 4;
        assert spread(1, 100, 0, 5, 60, .7) < 1;
        assert accuracyFloor(100, 100, 20) == 0 && accuracyFloor(101, 100, 20) == 1;
        System.out.println("Natural gunner difficulty and time accuracy floors passed");
    }
}
