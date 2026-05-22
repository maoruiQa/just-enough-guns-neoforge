package ttv.migami.jeg.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

public final class GunnerType {
    private GunnerType() {}

    public static String keyFor(PathfinderMob mob) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if ("minecraft:parched".equals(entityId)) {
            return "parched";
        }
        if (mob instanceof Husk) {
            return "husk";
        }
        if (mob instanceof ZombifiedPiglin) {
            return "zombifiedPiglin";
        }
        if (mob instanceof ZombieVillager) {
            return "zombieVillager";
        }
        if (mob instanceof Drowned) {
            return "drowned";
        }
        if (mob instanceof Zombie) {
            return "zombie";
        }
        if (mob instanceof WitherSkeleton) {
            return "witherSkeleton";
        }
        if (mob instanceof Stray) {
            return "stray";
        }
        if (mob instanceof Skeleton) {
            return "skeleton";
        }
        if (mob instanceof PiglinBrute) {
            return "piglinBrute";
        }
        if (mob instanceof Piglin) {
            return "piglin";
        }
        if (mob instanceof Vindicator) {
            return "vindicator";
        }
        if (mob instanceof Pillager) {
            return "pillager";
        }
        return "generic";
    }
}
