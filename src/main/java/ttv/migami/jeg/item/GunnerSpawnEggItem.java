package ttv.migami.jeg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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

        // Call parent method to handle standard spawn egg behavior
        mob.applyComponentsFromItemStack(stack);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, mob.position());
        stack.consume(1, player);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }
}