package ttv.migami.jeg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.stats.Stats;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.GunnerArmorEquiper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.faction.GunnerProgression;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

/**
 * Spawn egg that guarantees a JEG faction gunner when spawned.
 * Uses the JEG faction system to equip guns automatically.
 */
public class GunnerSpawnEggItem extends ModSpawnEggItem {

    public GunnerSpawnEggItem(EntityType<? extends Mob> type, Properties properties) {
        super(type, properties);
    }

    @Override
    protected void postSpawn(Level level, Mob mob, ItemStack stack, Player player) {
        // Add JEG gunner tag - the faction system will handle gun equipping automatically
        mob.addTag(GunEvents.JEG_GUNNER_TAG);
        GunnerMobSpawner.normalizeGunnerMob(mob);
        equipGunnerImmediately(mob);

        // Call parent method to handle standard spawn egg behavior (without applyComponentsFromItemStack)
        level.gameEvent(player, GameEvent.ENTITY_PLACE, mob.position());
        stack.consume(1, player);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    private static void equipGunnerImmediately(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinderMob) || mob.isBaby()) {
            return;
        }

        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (entityTypeId == null) {
            return;
        }

        Faction faction = GunnerManager.getInstance().getFactionForMob(entityTypeId);
        if (faction == null) {
            return;
        }

        boolean closeRange = mob.getRandom().nextBoolean();
        var gunItem = faction.getRandomGun(closeRange, mob.level(), mob.getRandom());
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

        mob.setItemSlot(EquipmentSlot.MAINHAND, gunStack);
        GunnerProgression.prepareDroppedWeapon(mob, gunStack);
        GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.normal(pathfinderMob));
        GunnerMobSpawner.reassessWeaponGoal(pathfinderMob);
        GunnerMobSpawner.extendFollowRange(pathfinderMob);
    }
}
