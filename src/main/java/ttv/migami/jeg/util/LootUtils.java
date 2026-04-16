package ttv.migami.jeg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModItems;

/**
 * Utility methods for applying loot tables to containers at runtime.
 */
public final class LootUtils {
    private LootUtils() {}

    private static final net.minecraft.resources.ResourceLocation SKY_SHIP_LOOT = Reference.id("chests/sky_ship_armada");
    private static final net.minecraft.resources.ResourceLocation SUPPLY_LOOT = Reference.id("chests/terror_phantom_supply");
    private static final net.minecraft.resources.ResourceLocation REWARD_LOOT = Reference.id("chests/terror_phantom_reward");
    private static final net.minecraft.resources.ResourceLocation FACTION_RAID_REWARD_LOOT = Reference.id("chests/faction_raid_reward");
    private static final ResourceLocation PHANTOM_SMG_ID = Reference.id("phantom_smg");
    private static final ResourceLocation FINGER_GUN_ID = Reference.id("finger_gun");
    private static final ResourceLocation ABSTRACT_GUN_ID = Reference.id("abstract_gun");
    private static final Set<ResourceLocation> EXCLUDED_GUN_LOOT = Set.of(PHANTOM_SMG_ID, FINGER_GUN_ID, ABSTRACT_GUN_ID);
    private static final Item[] DIAMOND_ARMOR = {
        Items.DIAMOND_HELMET,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_BOOTS
    };
    private static final Item[] IRON_ARMOR = {
        Items.IRON_HELMET,
        Items.IRON_CHESTPLATE,
        Items.IRON_LEGGINGS,
        Items.IRON_BOOTS
    };

    public static void fillContainer(ServerLevelAccessor level, BlockPos pos, ResourceKey<LootTable> lootTable, RandomSource random) {
        if (level == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            JustEnoughGuns.LOGGER.debug("[LootUtils] Missing block entity at {} when filling {}", pos, lootTable.location());
            return;
        }

        ServerLevel serverLevel = level.getLevel();
        boolean present = false;
        if (serverLevel != null && serverLevel.getServer() != null) {
            var registryHolder = serverLevel.getServer().reloadableRegistries();
            var lootLookup = registryHolder.lookup().lookup(Registries.LOOT_TABLE);
            present = lootLookup.flatMap(provider -> provider.get(lootTable)).isPresent();
        }
        JustEnoughGuns.LOGGER.debug("[LootUtils] Registry contains {} = {}", lootTable.location(), present);

        long seed = random.nextLong();

        if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {
            if (!present) {
                if (fillFallbackLoot(level, lootTable, randomizable, random)) {
                    randomizable.setChanged();
                }
                return;
            }
            randomizable.setLootTable(lootTable);
            randomizable.setLootTableSeed(seed);
            randomizable.setChanged();
            JustEnoughGuns.LOGGER.debug("[LootUtils] Assigned loot table {} seed={} to {}", lootTable.location(), seed, pos);
            return;
        }

        if (!(blockEntity instanceof Container container)) {
            JustEnoughGuns.LOGGER.debug("[LootUtils] Block entity at {} is not a container when filling {}", pos, lootTable.location());
            return;
        }

        if (!present) {
            if (fillFallbackLoot(level, lootTable, container, random) && blockEntity instanceof BlockEntity be) {
                be.setChanged();
            }
            return;
        }

        if (serverLevel == null || serverLevel.getServer() == null) {
            JustEnoughGuns.LOGGER.debug("[LootUtils] Missing server when filling {} at {}", lootTable.location(), pos);
            return;
        }

        var registryHolder = serverLevel.getServer().reloadableRegistries();
        LootTable table = registryHolder.getLootTable(lootTable);
        if (table == LootTable.EMPTY) {
            JustEnoughGuns.LOGGER.debug("[LootUtils] Loot table {} is empty when filling {}", lootTable.location(), pos);
            return;
        }

        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .create(LootContextParamSets.CHEST);
        container.clearContent();
        table.fill(container, params, seed);
        removeExcludedGunLoot(container);
        boolean empty = isContainerEmpty(container);
        JustEnoughGuns.LOGGER.debug("[LootUtils] Filled container {} with {} empty={} seed={}", pos, lootTable.location(), empty, seed);
        blockEntity.setChanged();
    }

    public static boolean isContainerEmpty(Container container) {
        return IntStream.range(0, container.getContainerSize()).allMatch(i -> container.getItem(i).isEmpty());
    }

    private static boolean fillFallbackLoot(ServerLevelAccessor level, ResourceKey<LootTable> lootTable, Container container, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        if (serverLevel == null) {
            return false;
        }

        HolderLookup.RegistryLookup<Enchantment> enchantLookup = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        net.minecraft.resources.ResourceLocation id = lootTable.location();

        if (id.equals(SKY_SHIP_LOOT)) {
            fillSkyShipFallback(container, random, enchantLookup);
            return true;
        }
        if (id.equals(SUPPLY_LOOT)) {
            fillSupplyFallback(container, random, enchantLookup);
            return true;
        }
        if (id.equals(REWARD_LOOT)) {
            fillRewardFallback(container, random, enchantLookup);
            return true;
        }
        if (id.equals(FACTION_RAID_REWARD_LOOT)) {
            fillFactionRaidRewardFallback(container, random, enchantLookup);
            return true;
        }
        return false;
    }

    private static void removeExcludedGunLoot(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (EXCLUDED_GUN_LOOT.contains(id)) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void fillSkyShipFallback(Container container, RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        container.clearContent();
        populateHighTierBundle(container, random, lookup);

        int extraRolls = 3 + random.nextInt(3);
        for (int i = 0; i < extraRolls; i++) {
            ItemStack stack = createSkyShipLootItem(random, lookup);
            if (!stack.isEmpty()) {
                placeInRandomSlot(container, stack, random);
            }
        }

        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    private static void populateHighTierBundle(Container container, RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemStack valuables = createSkyShipHighValue(random, lookup);
        if (!valuables.isEmpty()) {
            placeInRandomSlot(container, valuables, random);
        }

        ItemStack armor = createEnchantedArmor(random, lookup);
        if (!armor.isEmpty()) {
            placeInRandomSlot(container, armor, random);
        }

        ItemStack gun = createRandomGun(random);
        if (gun.isEmpty()) {
            gun = createRandomAmmo(random, 4, 9);
        }
        if (!gun.isEmpty()) {
            placeInRandomSlot(container, gun, random);
        }

        ItemStack ammo = createRandomAmmo(random, 4, 9);
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(3));
        }
        placeInRandomSlot(container, ammo, random);

        ItemStack gunpowder = new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(3));
        placeInRandomSlot(container, gunpowder, random);
    }

    private static ItemStack createSkyShipLootItem(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        int roll = random.nextInt(100);
        if (roll < 12) {
            return new ItemStack(Items.PHANTOM_MEMBRANE, 2 + random.nextInt(2));
        }
        if (roll < 24) {
            return new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(2));
        }
        if (roll < 36) {
            return new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(3));
        }
        if (roll < 48) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE, 2 + random.nextInt(2));
        }
        if (roll < 62) {
            return createEnchantedBook(random, lookup);
        }
        if (roll < 75) {
            return createEnchantedArmor(random, lookup);
        }
        if (roll < 88) {
            return createRandomGun(random);
        }
        if (roll < 96) {
            return createRandomAmmo(random, 4, 9);
        }
        return new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(2));
    }

    private static ItemStack createSkyShipHighValue(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        int roll = random.nextInt(100);
        if (roll < 35) {
            return new ItemStack(Items.DIAMOND, 3 + random.nextInt(3));
        }
        if (roll < 65) {
            return new ItemStack(Items.EMERALD, 4 + random.nextInt(3));
        }
        if (roll < 85) {
            return new ItemStack(Items.GOLD_INGOT, 6 + random.nextInt(5));
        }
        if (roll < 95) {
            return new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(2));
        }
        return createEnchantedBook(random, lookup);
    }

    private static void fillSupplyFallback(Container container, RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        container.clearContent();

        // Core bundle ensures valuables, armor, gun, and ammunition
        populateHighTierBundle(container, random, lookup);

        int rolls = 5 + random.nextInt(3);
        for (int i = 0; i < rolls; i++) {
            ItemStack stack = createSupplyLootItem(random);
            if (!stack.isEmpty()) {
                placeInRandomSlot(container, stack, random);
            }
        }

        ItemStack bonusAmmo = createRandomAmmo(random, 48, 96);
        if (!bonusAmmo.isEmpty()) {
            placeInRandomSlot(container, bonusAmmo, random);
        }

        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    private static ItemStack createSupplyLootItem(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 20) {
            return createRandomAmmo(random, 32, 64);
        }
        if (roll < 40) {
            return new ItemStack(Items.GUNPOWDER, 8 + random.nextInt(5));
        }
        if (roll < 55) {
            return new ItemStack(Items.TNT, 1 + random.nextInt(2));
        }
        if (roll < 70) {
            return new ItemStack(Items.STRING, 6 + random.nextInt(5));
        }
        if (roll < 85) {
            return new ItemStack(Items.IRON_INGOT, 6 + random.nextInt(5));
        }
        if (roll < 95) {
            return new ItemStack(Items.BLAZE_POWDER, 4 + random.nextInt(3));
        }
        return new ItemStack(Items.EMERALD, 2 + random.nextInt(3));
    }

    private static void fillRewardFallback(Container container, RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        container.clearContent();
        populateHighTierBundle(container, random, lookup);

        ItemStack bonusBook = createEnchantedBook(random, lookup);
        if (!bonusBook.isEmpty()) {
            placeInRandomSlot(container, bonusBook, random);
        }

        ItemStack bonusArmor = createEnchantedArmor(random, lookup);
        if (!bonusArmor.isEmpty()) {
            placeInRandomSlot(container, bonusArmor, random);
        }

        int extraRolls = 4 + random.nextInt(3);
        for (int i = 0; i < extraRolls; i++) {
            ItemStack stack = createRewardLootItem(random, lookup);
            if (!stack.isEmpty()) {
                placeInRandomSlot(container, stack, random);
            }
        }
        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    private static ItemStack createRewardLootItem(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        int roll = random.nextInt(100);
        if (roll < 18) {
            return new ItemStack(Items.DIAMOND, 4 + random.nextInt(3));
        }
        if (roll < 38) {
            return createEnchantedBook(random, lookup);
        }
        if (roll < 60) {
            return createEnchantedArmor(random, lookup);
        }
        if (roll < 78) {
            return createRandomGun(random);
        }
        if (roll < 92) {
            return createRandomAmmo(random, 64, 96);
        }
        if (roll < 98) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        }
        return new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(2));
    }

    private static void fillFactionRaidRewardFallback(Container container, RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        container.clearContent();

        ItemStack primaryGun = createRandomGun(random);
        if (!primaryGun.isEmpty()) {
            placeInRandomSlot(container, primaryGun, random);
        }

        ItemStack secondaryGun = createRandomGun(random);
        if (!secondaryGun.isEmpty()) {
            placeInRandomSlot(container, secondaryGun, random);
        }

        for (int i = 0; i < 2; i++) {
            ItemStack ammo = createRandomAmmo(random, 20, 40);
            if (!ammo.isEmpty()) {
                placeInRandomSlot(container, ammo, random);
            }
        }

        ItemStack primaryArmor = createHighTierEnchantedArmor(random, lookup);
        if (!primaryArmor.isEmpty()) {
            placeInRandomSlot(container, primaryArmor, random);
        }

        ItemStack bonusArmor = createHighTierEnchantedArmor(random, lookup);
        if (!bonusArmor.isEmpty()) {
            placeInRandomSlot(container, bonusArmor, random);
        }

        ItemStack guaranteedBook = createHighTierEnchantedBook(random, lookup);
        if (!guaranteedBook.isEmpty()) {
            placeInRandomSlot(container, guaranteedBook, random);
        }

        int extraRolls = 6 + random.nextInt(3);
        for (int i = 0; i < extraRolls; i++) {
            ItemStack stack = createFactionRaidRewardLootItem(random, lookup);
            if (!stack.isEmpty()) {
                placeInRandomSlot(container, stack, random);
            }
        }
        if (container instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
    }

    private static ItemStack createFactionRaidRewardLootItem(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        int roll = random.nextInt(100);
        if (roll < 16) {
            return createRandomAmmo(random, 20, 36);
        }
        if (roll < 30) {
            return createRandomGun(random);
        }
        if (roll < 46) {
            return createHighTierEnchantedBook(random, lookup);
        }
        if (roll < 62) {
            return createHighTierEnchantedArmor(random, lookup);
        }
        if (roll < 74) {
            return new ItemStack(Items.DIAMOND, 3 + random.nextInt(4));
        }
        if (roll < 84) {
            return new ItemStack(Items.EMERALD, 6 + random.nextInt(7));
        }
        if (roll < 92) {
            return new ItemStack(Items.GOLDEN_APPLE, 2 + random.nextInt(2));
        }
        if (roll < 97) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        }
        return new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(3));
    }

    private static ItemStack createRandomGun(RandomSource random) {
        if (ModItems.GUNS.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<Item> guns = new ArrayList<>();
        ModItems.GUNS.forEach((id, holder) -> {
            if (EXCLUDED_GUN_LOOT.contains(id)) {
                return;
            }
            guns.add(holder.get());
        });
        if (guns.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = guns.get(random.nextInt(guns.size()));
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int damage = random.nextInt(Math.max(1, maxDamage / 3));
            stack.setDamageValue(damage);
        }
        return stack;
    }

    private static ItemStack createRandomAmmo(RandomSource random, int min, int max) {
        if (ModItems.AMMO.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<Item> ammoItems = new ArrayList<>();
        ModItems.AMMO.values().forEach(holder -> ammoItems.add(holder.get()));
        Item item = ammoItems.get(random.nextInt(ammoItems.size()));
        int count = Mth.nextInt(random, min, max);
        return new ItemStack(item, count);
    }

    private static ItemStack createHighTierEnchantedArmor(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        Item item = DIAMOND_ARMOR[random.nextInt(DIAMOND_ARMOR.length)];
        ItemStack stack = new ItemStack(item);
        List<Holder.Reference<Enchantment>> available = lookup.listElements()
            .filter(ref -> ref.value().canEnchant(stack))
            .collect(Collectors.toCollection(ArrayList::new));
        if (available.isEmpty()) {
            return stack;
        }
        int enchantments = 3 + random.nextInt(2);
        for (int i = 0; i < enchantments && !available.isEmpty(); i++) {
            Holder.Reference<Enchantment> holder = available.remove(random.nextInt(available.size()));
            Enchantment enchantment = holder.value();
            int minLevel = enchantment.getMinLevel();
            int maxLevel = enchantment.getMaxLevel();
            int level = maxLevel;
            if (maxLevel > minLevel) {
                int lowerBound = Math.max(minLevel, maxLevel - 1);
                level = Mth.nextInt(random, lowerBound, maxLevel);
            }
            stack.enchant(holder, level);
        }
        return stack;
    }

    private static ItemStack createEnchantedArmor(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        boolean chooseDiamond = random.nextFloat() < 0.6F;
        Item[] pool = chooseDiamond ? DIAMOND_ARMOR : IRON_ARMOR;
        Item item = pool[random.nextInt(pool.length)];
        ItemStack stack = new ItemStack(item);
        List<Holder.Reference<Enchantment>> available = lookup.listElements()
            .filter(ref -> ref.value().canEnchant(stack))
            .collect(Collectors.toCollection(ArrayList::new));
        if (available.isEmpty()) {
            return stack;
        }
        int enchantments = chooseDiamond ? 2 + random.nextInt(2) : 1 + random.nextInt(2);
        for (int i = 0; i < enchantments && !available.isEmpty(); i++) {
            Holder.Reference<Enchantment> holder = available.remove(random.nextInt(available.size()));
            Enchantment enchantment = holder.value();
            int minLevel = enchantment.getMinLevel();
            int maxLevel = enchantment.getMaxLevel();
            int level = maxLevel;
            if (maxLevel > minLevel) {
                int lowerBound = Math.max(minLevel, maxLevel - 1);
                level = Mth.nextInt(random, lowerBound, maxLevel);
            }
            stack.enchant(holder, level);
        }
        return stack;
    }

    private static ItemStack createHighTierEnchantedBook(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        List<Holder.Reference<Enchantment>> enchantments = lookup.listElements()
            .filter(ref -> ref.value().getMaxLevel() >= 3)
            .collect(Collectors.toCollection(ArrayList::new));
        if (enchantments.isEmpty()) {
            return createEnchantedBook(random, lookup);
        }
        Holder.Reference<Enchantment> holder = enchantments.get(random.nextInt(enchantments.size()));
        Enchantment enchantment = holder.value();
        int minLevel = enchantment.getMinLevel();
        int maxLevel = enchantment.getMaxLevel();
        int lowerBound = Math.max(minLevel, maxLevel - 1);
        int level = Mth.nextInt(random, lowerBound, maxLevel);
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.enchant(holder, level);
        return stack;
    }

    private static ItemStack createEnchantedBook(RandomSource random, HolderLookup.RegistryLookup<Enchantment> lookup) {
        List<Holder.Reference<Enchantment>> enchantments = lookup.listElements()
            .collect(Collectors.toCollection(ArrayList::new));
        if (enchantments.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Holder.Reference<Enchantment> holder = enchantments.get(random.nextInt(enchantments.size()));
        Enchantment enchantment = holder.value();
        int minLevel = enchantment.getMinLevel();
        int maxLevel = enchantment.getMaxLevel();
        int level = maxLevel;
        if (maxLevel > minLevel) {
            int lowerBound = Math.max(minLevel, maxLevel - 1);
            level = Mth.nextInt(random, lowerBound, maxLevel);
        }
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.enchant(holder, level);
        return stack;
    }

    private static void placeInRandomSlot(Container container, ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || container.getContainerSize() == 0) {
            return;
        }
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                empty.add(i);
            }
        }
        int slot = empty.isEmpty() ? random.nextInt(container.getContainerSize()) : empty.get(random.nextInt(empty.size()));
        container.setItem(slot, stack);
    }
}
