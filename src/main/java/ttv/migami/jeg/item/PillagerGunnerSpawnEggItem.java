package ttv.migami.jeg.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerArmorEquiper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.GunnerProgression;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.faction.GunnerType;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;

/**
 * Spawn egg for pillager gunners.
 * Spawns a Pillager with gun equipment and reduced spread for better accuracy.
 */
public class PillagerGunnerSpawnEggItem extends ModSpawnEggItem {

    public PillagerGunnerSpawnEggItem(EntityType<? extends Mob> type, Properties properties) {
        super(type, properties);
    }

    @Override
    protected void postSpawn(Level level, Mob mob, ItemStack stack, Player player) {
        RandomSource random = mob.level().getRandom();

        // Only handle Pillager spawns
        if (mob instanceof Pillager pillager) {
            // Add special tag to identify as pillager gunner (for reduced spread)
            pillager.addTag("jeg_pillager_gunner");
            GunnerMobSpawner.normalizeGunnerMob(pillager);

            // Equip with a random gun from pillager gun pool
            equipPillagerWithGun(pillager, random);
        }

        // Call parent method to handle standard spawn egg behavior (without applyComponentsFromItemStack)
        level.gameEvent(player, GameEvent.ENTITY_PLACE, mob.position());
        stack.consume(1, player);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    /**
     * Equips a pillager gunner with a random gun from the enhanced pillager weapon pool
     */
    private void equipPillagerWithGun(Pillager pillager, RandomSource random) {
        PathfinderMob pathfinderMob = pillager;
        Faction faction = GunnerManager.getInstance().getFactionForMob(BuiltInRegistries.ENTITY_TYPE.getKey(pillager.getType()));
        if (faction == null) {
            return;
        }
        String gunnerType = GunnerType.keyFor(pillager);
        boolean elite = GunMobValues.rollElite(pillager.level(), random);
        var gunItem = elite
                ? faction.getEliteGun(pillager.level(), random, gunnerType)
                : faction.getRandomGun(random.nextBoolean(), pillager.level(), random, gunnerType);
        if (gunItem == null) {
            return;
        }
        ItemStack gunStack = new ItemStack(gunItem);
        if (gunItem instanceof GunItem gun) {
            GunStats stats = gun.getStats();
            if (gun.usesLoadedAmmo()) {
                gunStack.set(ModDataComponents.GUN_AMMO.get(), Math.max(1, stats.magazineSize()));
            }
        }
        pillager.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
        if (elite) {
            GunnerMobSpawner.applyEliteAttributes(pathfinderMob);
        }
        GunnerProgression.prepareDroppedWeapon(pillager, gunStack);
        GunnerArmorEquiper.equipGunnerArmor(random, elite
                ? GunnerArmorEquiper.GunnerArmorContext.elite(pathfinderMob)
                : GunnerArmorEquiper.GunnerArmorContext.normal(pathfinderMob));
        GunnerMobSpawner.reassessWeaponGoal(pathfinderMob);
        GunnerMobSpawner.extendFollowRange(pathfinderMob);
        pillager.setCanPickUpLoot(false);
    }
}
