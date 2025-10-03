package ttv.migami.jeg.init;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.ManualItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(Registries.ITEM, Reference.MOD_ID);

    public static final Map<ResourceLocation, DeferredHolder<Item, Item>> AMMO = new LinkedHashMap<>();
    public static final Map<ResourceLocation, DeferredHolder<Item, GunItem>> GUNS = new LinkedHashMap<>();
    private static final List<ResourceKey<Recipe<?>>> MANUAL_RECIPES;

    private static final Set<String> AMMO_IDS = Set.of(
            "pistol_ammo",
            "rifle_ammo",
            "shotgun_shell",
            "handmade_shell",
            "spectre_round",
            "blaze_round",
            "pocket_bubble",
            "water_bomb",
            "rocket",
            "grenade",
            "flare"
    );

    static {
        registerAmmoItems();
        registerGunItems();
        MANUAL_RECIPES = buildManualRecipes();
    }

    public static final DeferredHolder<Item, ManualItem> GUNSMITH_MANUAL = REGISTER.register(
            "gunsmith_manual",
            () -> new ManualItem(baseProperties(Reference.id("gunsmith_manual")).stacksTo(1), MANUAL_RECIPES)
    );

    private static void registerAmmoItems() {
        for (String path : AMMO_IDS) {
            ResourceLocation id = Reference.id(path);
            if ("grenade".equals(path)) {
                AMMO.put(id, REGISTER.register(path, () -> new GrenadeItem(baseProperties(id).stacksTo(16))));
            } else {
                AMMO.put(id, REGISTER.register(path, () -> new Item(baseProperties(id))));
            }
        }
    }

    private static void registerGunItems() {
        for (Map.Entry<ResourceLocation, GunStats> entry : GunDefinitions.ALL.entrySet()) {
            ResourceLocation id = entry.getKey();
            GunStats stats = entry.getValue();
            DeferredHolder<Item, GunItem> holder = REGISTER.register(id.getPath(), () -> new GunItem(defaultGunProperties(id, stats), stats));
            GUNS.put(id, holder);
        }
    }

    private static List<ResourceKey<Recipe<?>>> buildManualRecipes() {
        java.util.ArrayList<ResourceKey<Recipe<?>>> keys = new java.util.ArrayList<>();
        for (String ammo : AMMO_IDS) {
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id(ammo)));
        }
        for (ResourceLocation id : GunDefinitions.ALL.keySet()) {
            keys.add(ResourceKey.create(Registries.RECIPE, id));
        }
        return List.copyOf(keys);
    }

    public static List<ResourceKey<Recipe<?>>> manualRecipes() {
        return MANUAL_RECIPES;
    }

    private static Item.Properties defaultGunProperties(ResourceLocation id, GunStats stats) {
        int durability = switch (stats.reloadType()) {
            case "jeg:inventory_fed" -> 1024;
            case "jeg:manual" -> 256;
            default -> 512;
        };
        return baseProperties(id).stacksTo(1).durability(durability);
    }

    private static Item.Properties baseProperties(ResourceLocation id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            GUNS.values().forEach(holder -> event.accept(holder.get()));
            AMMO.values().forEach(holder -> event.accept(holder.get()));
        }

        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(GUNSMITH_MANUAL.get());
        }
    }
}
