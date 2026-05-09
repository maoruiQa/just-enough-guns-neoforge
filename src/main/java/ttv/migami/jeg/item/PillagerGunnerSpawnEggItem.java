package ttv.migami.jeg.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
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
import ttv.migami.jeg.faction.GunnerProgression;
import ttv.migami.jeg.faction.GunnerMobSpawner;
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
        // Use the same enhanced weapon pool as regular pillager gunners
        ResourceLocation[] guns = new ResourceLocation[] {
                ttv.migami.jeg.Reference.id("assault_rifle"),
                ttv.migami.jeg.Reference.id("burst_rifle"),
                ttv.migami.jeg.Reference.id("service_rifle"),
                ttv.migami.jeg.Reference.id("light_machine_gun"),
                ttv.migami.jeg.Reference.id("semi_auto_rifle"),
                ttv.migami.jeg.Reference.id("combat_rifle"),
                ttv.migami.jeg.Reference.id("infantry_rifle"),
                ttv.migami.jeg.Reference.id("pump_shotgun"),
                ttv.migami.jeg.Reference.id("repeating_shotgun"),
                ttv.migami.jeg.Reference.id("minigun")
        };

        java.util.List<net.minecraft.world.item.Item> pool = new java.util.ArrayList<>();
        for (ResourceLocation gunId : guns) {
            var holder = ModItems.GUNS.get(gunId);
            if (holder != null) {
                pool.add(holder.get());
            }
        }
        if (!pool.isEmpty()) {
            ItemStack gunStack = new ItemStack(GunnerProgression.selectGun(pool, pillager.level(), random));
            pillager.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
            GunnerProgression.prepareDroppedWeapon(pillager, gunStack);
            pillager.setCanPickUpLoot(false);
        }
    }
}
