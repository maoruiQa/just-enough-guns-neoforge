package ttv.migami.jeg.faction.raid;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerProgression;
import ttv.migami.jeg.item.BulletproofArmorItem;
import ttv.migami.jeg.item.GunItem;

/** Immutable per-raid snapshot. The save includes values, rather than re-reading config on recovery. */
public record RaidDifficulty(int tier, int totalWaves, int waveBudget, int weaponTier,
                             int armorTier, double eliteChance, int rewardTier, String vehiclePlan) {
    private static final String MOB_TAG = "JEGDynamicRaid:";

    public static RaidDifficulty evaluate(List<? extends Player> players, double timeProgress) {
        double equipment = players.stream().mapToDouble(RaidDifficulty::equipmentScore).max().orElse(0);
        return forScore(equipment, timeProgress, players.size());
    }

    public static RaidDifficulty forScore(double equipment, double timeProgress, int players) {
        double score = Math.min(100, Mth.clamp(equipment, 0, 100) + 15 * Mth.clamp(timeProgress, 0, 1));
        int tier = Math.min(5, 1 + (int) (score / 20));
        int[] waves = {2, 3, 3, 4, 5};
        int[] budgets = {8, 12, 16, 20, 24};
        int[] armor = {1, 2, 3, 5, 6};
        String[] vehicles = {"00", "000", "011", "0122", "11223"};
        int budget = (int) Math.ceil(budgets[tier - 1] * (1 + .25 * Math.min(3, Math.max(0, players - 1))));
        return new RaidDifficulty(tier, waves[tier - 1], budget, Math.min(3, tier - 1),
                armor[tier - 1], (tier - 1) * .1, tier, vehicles[tier - 1]);
    }

    public int vehicleCount(int wave) {
        return wave < 1 || wave > vehiclePlan.length() ? 0 : vehiclePlan.charAt(wave - 1) - '0';
    }

    public List<String> vehiclePool(int wave, boolean heavyAlreadySpawned) {
        if (tier < 3 || vehicleCount(wave) == 0) return List.of();
        if (tier == 3) return List.of("lav150");
        if (tier == 5 && wave >= 3 && !heavyAlreadySpawned) return List.of("lav150", "bmp2", "ah6", "mi28");
        return List.of("lav150", "bmp2", "ah6");
    }

    public double rareRewardChance() {
        return rewardTier == 5 ? .35 : rewardTier == 4 ? .15 : 0;
    }

    public String save() {
        return "1," + tier + "," + totalWaves + "," + waveBudget + "," + weaponTier + "," + armorTier
                + "," + eliteChance + "," + rewardTier + "," + vehiclePlan;
    }

    @Nullable
    public static RaidDifficulty load(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String[] fields = value.split(",", -1);
            if (fields.length != 9 || !fields[0].equals("1")) return null;
            RaidDifficulty result = new RaidDifficulty(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
                    Double.parseDouble(fields[6]), Integer.parseInt(fields[7]), fields[8]);
            if (result.tier < 1 || result.tier > 5 || result.totalWaves < 1 || result.totalWaves > 5
                    || result.waveBudget < 1 || result.waveBudget > 64 || result.weaponTier < 0 || result.weaponTier > 3
                    || result.armorTier < 1 || result.armorTier > 6 || !Double.isFinite(result.eliteChance)
                    || result.eliteChance < 0 || result.eliteChance > 1 || result.rewardTier < 1 || result.rewardTier > 5
                    || result.vehiclePlan.length() != result.totalWaves || !result.vehiclePlan.matches("[0-3]+")) return null;
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void prepareMob(Mob mob) {
        mob.entityTags().stream().filter(tag -> tag.startsWith(MOB_TAG)).toList().forEach(mob::removeTag);
        mob.addTag(MOB_TAG + save());
        mob.removeTag("EliteGunner");
        if (GunMobValues.elitesEnabled && mob.getRandom().nextDouble() < eliteChance) mob.addTag("EliteGunner");
    }

    @Nullable
    public static RaidDifficulty forMob(Mob mob) {
        for (String tag : mob.entityTags()) if (tag.startsWith(MOB_TAG)) return load(tag.substring(MOB_TAG.length()));
        return null;
    }

    public static double equipmentScore(Player player) {
        double weapon = weaponScore(player, player.getOffhandItem());
        for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()); slot++) {
            weapon = Math.max(weapon, weaponScore(player, player.getInventory().getItem(slot)));
        }
        double armor = 0, toughness = 0;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (usable(stack) && !(stack.getItem() instanceof BulletproofArmorItem)) {
                armor += stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ARMOR, 0, slot);
                toughness += stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ARMOR_TOUGHNESS, 0, slot);
            }
        }
        double ballistic = .4 * bulletproofScore(player.getItemBySlot(EquipmentSlot.HEAD))
                + .6 * bulletproofScore(player.getItemBySlot(EquipmentSlot.CHEST));
        double protection = Mth.clamp(ballistic + 100 * (armor / 30 + toughness / 40), 0, 100);
        return Mth.clamp(.7 * weapon + .3 * protection, 0, 100);
    }

    private static double bulletproofScore(ItemStack stack) {
        return usable(stack) && stack.getItem() instanceof BulletproofArmorItem armor ? armor.tier().tierNumber() * 100.0 / 6 : 0;
    }

    private static boolean usable(ItemStack stack) {
        return !stack.isEmpty() && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
    }

    private static double weaponScore(Player player, ItemStack stack) {
        if (!usable(stack)) return 0;
        if (stack.getItem() instanceof GunItem gun) {
            if (!hasAmmo(player, stack, gun)) return 0;
            String id = gun.getStats().id().getPath();
            if (id.equals("rocket_launcher") || id.equals("javelin") || id.equals("igla_9k38")) return 100;
            if (gun.getStats().damage() <= 0) return 0;
            return 25 * (1 + GunnerProgression.weaponTier(gun));
        }
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
            boolean loaded = !stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).isEmpty();
            if (!loaded && !hasItem(player, Items.ARROW) && !hasItem(player, Items.SPECTRAL_ARROW) && !hasItem(player, Items.TIPPED_ARROW)) return 0;
            return stack.getItem() instanceof CrossbowItem ? 50 : 40;
        }
        // ponytail: unknown mod weapons use their public attack attributes; add mod adapters only for a proven mismatch.
        double damage = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND);
        return Double.isFinite(damage) ? 100 * Mth.clamp((damage - 1) / 10, 0, 1) : 0;
    }

    /** Strong equipment and usable anti-vehicle weapons outweigh distance among visible targets. */
    public static double threatPriority(Player player, boolean airborneEnemy) {
        double score = equipmentScore(player);
        if (ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat.combatMounted(player)) {
            score = Math.max(score, 100 * ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat.mountStrength(player)) + 40;
        }
        if (antiVehicleWeapon(player, player.getOffhandItem(), airborneEnemy)) return score + 40;
        for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()); slot++) {
            if (antiVehicleWeapon(player, player.getInventory().getItem(slot), airborneEnemy)) return score + 40;
        }
        return score;
    }

    private static boolean antiVehicleWeapon(Player player, ItemStack stack, boolean airborneEnemy) {
        if (!usable(stack) || !(stack.getItem() instanceof GunItem gun) || !hasAmmo(player, stack, gun)) return false;
        String id = gun.getStats().id().getPath();
        return id.equals("rocket_launcher") || id.equals("javelin") || airborneEnemy && id.equals("igla_9k38");
    }

    private static boolean hasAmmo(Player player, ItemStack stack, GunItem gun) {
        if (gun.getMagazineAmmo(stack) > 0 || gun.getMagazineInventorySummary(player).loadedMagazineCount() > 0
                || gun.isLoadedMagazineCompatible(player.getOffhandItem())) return true;
        var ammoId = gun.getStats().ammoItem();
        if (ammoId == null || ammoId.getPath().equals("air")) return true;
        return BuiltInRegistries.ITEM.getOptional(ammoId).map(ammo -> hasItem(player, ammo)).orElse(false);
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item) {
        if (player.getOffhandItem().is(item)) return true;
        for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()); slot++) {
            if (player.getInventory().getItem(slot).is(item)) return true;
        }
        return false;
    }

    /** Gradle checkRaidBalance runs these without booting a game world. */
    public static void main(String[] args) {
        assert forScore(0, 1, 1).tier == 1;
        assert forScore(19.99, 0, 1).tier == 1;
        assert forScore(20, 0, 1).tier == 2;
        assert forScore(40, 0, 1).tier == 3;
        assert forScore(60, 0, 1).tier == 4;
        assert forScore(80, 0, 1).tier == 5;
        assert forScore(100, 1, 100).waveBudget == 42;
        assert forScore(0, 100, 1).tier == 1;
        assert forScore(0, 0, 2).waveBudget == 10;
        assert forScore(0, 0, 4).waveBudget == 14;
        assert forScore(0, 0, 100).waveBudget == 14;
        assert forScore(0, -1, 1).tier == 1;
        assert load("broken") == null;
        for (int tier = 1; tier <= 5; tier++) {
            RaidDifficulty profile = forScore((tier - 1) * 20, 0, 1);
            assert profile.equals(load(profile.save()));
            assert profile.rareRewardChance() == (tier == 5 ? .35 : tier == 4 ? .15 : 0);
            assert !profile.vehiclePool(2, false).contains("mi28");
            assert !profile.vehiclePool(3, true).contains("mi28");
            if (tier < 3) assert profile.vehicleCount(1) == 0 && profile.vehiclePool(1, false).isEmpty();
        }
        ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat.selfCheck();
        ttv.migami.jeg.faction.NaturalGunnerDifficulty.selfCheck();
        checkRewardTables();
        System.out.println("Raid and enemy vehicle balance checks passed");
    }

    private static void checkRewardTables() {
        for (int tier = 1; tier <= 5; tier++) {
            String path = "/data/jeg/loot_table/chests/faction_raid_reward_" + tier + ".json";
            try (var stream = RaidDifficulty.class.getResourceAsStream(path)) {
                assert stream != null : "Missing loaded reward resource " + path;
                var pools = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonArray("pools");
                var profile = forScore((tier - 1) * 20, 0, 1);
                for (var element : pools.get(0).getAsJsonObject().getAsJsonArray("entries")) {
                    var entry = element.getAsJsonObject();
                    String name = entry.get("name").getAsString();
                    String armorSuffix = new String[]{"i", "ii", "iii", "v", "vi"}[tier - 1];
                    if (name.startsWith("jeg:bulletproof_")) {
                        assert name.endsWith("_" + armorSuffix) || tier == 3 && name.endsWith("_iv") : name;
                    } else {
                        var stats = ttv.migami.jeg.gun.GunDefinitions.ALL.get(ttv.migami.jeg.Reference.id(name.substring(4)));
                        assert tier < 5 && stats != null && stats.damage() > 0 && GunnerProgression.weaponTier(stats) == profile.weaponTier : name;
                        assert !List.of("rocket_launcher", "javelin", "igla_9k38", "phantom_smg", "abstract_gun", "typhoonee").contains(name.substring(4)) : name;
                    }
                    assert entry.getAsJsonArray("functions").get(0).getAsJsonObject().get("damage").getAsDouble() == 1;
                }
                assert pools.size() == (tier >= 4 ? 5 : 4);
                if (tier >= 4) {
                    var rare = pools.get(4).getAsJsonObject();
                    assert Math.abs(rare.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble() - profile.rareRewardChance()) < .000001;
                    for (var entry : rare.getAsJsonArray("entries")) {
                        assert List.of("jeg:rocket_launcher", "jeg:minigun", "jeg:light_machine_gun").contains(entry.getAsJsonObject().get("name").getAsString());
                    }
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
    }
}
