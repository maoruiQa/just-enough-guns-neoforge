package ttv.migami.jeg.faction;

import net.minecraft.world.item.Item;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Faction {
    private final String name;
    private final int aiLevel;
    private final List<String> mobs;
    private final List<Item> closeGuns;
    private final List<Item> longGuns;
    private final List<Item> eliteGuns;

    public Faction(String name, int aiLevel, List<String> mobs, List<Item> closeGuns, List<Item> longGuns, List<Item> eliteGuns) {
        this.name = name;
        this.aiLevel = aiLevel;
        this.mobs = mobs;
        this.closeGuns = closeGuns;
        this.longGuns = longGuns;
        this.eliteGuns = eliteGuns;
    }

    public String getName() {
        return name;
    }

    public int getAiLevel() {
        return aiLevel;
    }

    public List<String> getMobs() {
        return mobs;
    }

    public Item getRandomGun(boolean isCloseRange) {
        return getRandomGun(isCloseRange, null, null);
    }

    public Item getRandomGun(boolean isCloseRange, Level level, RandomSource random) {
        return getRandomGun(isCloseRange, level, random, "generic");
    }

    public Item getRandomGun(boolean isCloseRange, Level level, RandomSource random, String gunnerType) {
        List<Item> pool = isCloseRange ? closeGuns : longGuns;
        if (pool.isEmpty()) {
            return null;
        }
        if (level != null && random != null) {
            return GunnerProgression.selectGun(pool, level, random, gunnerType);
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    public Item getEliteGun() {
        return getEliteGun(null, null);
    }

    public Item getEliteGun(Level level, RandomSource random) {
        return getEliteGun(level, random, "generic");
    }

    public Item getEliteGun(Level level, RandomSource random, String gunnerType) {
        List<Item> pool = eliteGuns;
        if (pool.isEmpty()) {
            return null;
        }
        if (level != null && random != null) {
            return GunnerProgression.selectGun(pool, level, random, gunnerType);
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
