package ttv.migami.jeg.faction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModItems;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GunnerManager {
    private static GunnerManager instance;
    private final Map<String, Faction> factions = new HashMap<>();
    static List<String> gunnerMobsList = null;

    public static List<String> getConfigFactions() {
        if (gunnerMobsList == null) {
            // Factions configuration matching 1.20.1 reference
            gunnerMobsList = Arrays.asList(
                "night_of_the_undead|1|minecraft:zombie,minecraft:husk,minecraft:zombie_villager,minecraft:drowned|jeg:custom_smg,jeg:waterpipe_shotgun,jeg:double_barrel_shotgun,jeg:pump_shotgun,jeg:repeating_shotgun,jeg:light_machine_gun|jeg:revolver,jeg:semi_auto_rifle,jeg:combat_rifle,jeg:assault_rifle,jeg:service_rifle,jeg:light_machine_gun,jeg:minigun|jeg:double_barrel_shotgun,jeg:repeating_shotgun,jeg:light_machine_gun,jeg:minigun",
                "the_rattlers|2|minecraft:skeleton,minecraft:stray|jeg:custom_smg|jeg:semi_auto_rifle,jeg:combat_rifle,jeg:infantry_rifle|jeg:bolt_action_rifle,jeg:combat_rifle,jeg:infantry_rifle",
                "nosy_business|3|minecraft:pillager,minecraft:vindicator|jeg:pump_shotgun|jeg:semi_auto_rifle,jeg:assault_rifle|jeg:repeating_shotgun,jeg:service_rifle",
                "bad_piggies|2|minecraft:piglin,minecraft:piglin_brute|jeg:double_barrel_shotgun,jeg:waterpipe_shotgun|jeg:assault_rifle,jeg:semi_auto_rifle|jeg:hollenfire_mk2",
                "hell_hogs|3|minecraft:zombified_piglin,minecraft:wither_skeleton|jeg:custom_smg,jeg:waterpipe_shotgun,jeg:double_barrel_shotgun|jeg:assault_rifle,jeg:semi_auto_rifle|jeg:soulhunter_mk2",
                "lost_souls|3|jeg:ghoul|jeg:waterpipe_shotgun,jeg:revolver|jeg:custom_smg,jeg:assault_rifle,jeg:semi_auto_rifle|jeg:blossom_rifle,jeg:service_rifle,jeg:infantry_rifle"
            );
            ttv.migami.jeg.JustEnoughGuns.LOGGER.info("Registered JEG Factions: {}", gunnerMobsList);
        }
        return gunnerMobsList;
    }

    public static GunnerManager getInstance() {
        if (instance == null) {
            instance = new GunnerManager(getConfigFactions());
        }
        return instance;
    }

    public GunnerManager(List<? extends String> factionConfig) {
        for (String entry : factionConfig) {
            String[] parts = entry.split("\\|");
            if (parts.length == 6) {
                String name = parts[0];
                int aiLevel = Integer.parseInt(parts[1]);
                List<String> mobs = Arrays.asList(parts[2].split(","));
                List<Item> closeGuns = Arrays.stream(parts[3].split(","))
                        .map(gunName -> {
                            try {
                                ResourceLocation gunId = ResourceLocation.parse(gunName);
                                DeferredHolder<?, ?> holder = ModItems.GUNS.get(gunId);
                                return holder != null ? (Item) holder.get() : null;
                            } catch (Exception e) {
                                ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("Failed to resolve gun item: {}", gunName);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();
                List<Item> longGuns = Arrays.stream(parts[4].split(","))
                        .map(gunName -> {
                            try {
                                ResourceLocation gunId = ResourceLocation.parse(gunName);
                                DeferredHolder<?, ?> holder = ModItems.GUNS.get(gunId);
                                return holder != null ? (Item) holder.get() : null;
                            } catch (Exception e) {
                                ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("Failed to resolve gun item: {}", gunName);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();
                List<Item> eliteGuns = Arrays.stream(parts[5].split(","))
                        .map(gunName -> {
                            try {
                                ResourceLocation gunId = ResourceLocation.parse(gunName);
                                DeferredHolder<?, ?> holder = ModItems.GUNS.get(gunId);
                                return holder != null ? (Item) holder.get() : null;
                            } catch (Exception e) {
                                ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("Failed to resolve gun item: {}", gunName);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();

                factions.put(name, new Faction(name, aiLevel, mobs, closeGuns, longGuns, eliteGuns));
                // Faction loaded successfully
            }
        }
    }

    public Faction getFactionForMob(ResourceLocation entityType) {
        return factions.values().stream()
                .filter(f -> f.getMobs().contains(entityType.toString()))
                .findFirst().orElse(null);
    }

    public Faction getFactionByName(String factionName) {
        return getFactions().stream()
                .filter(f -> f.getName().equalsIgnoreCase(factionName))
                .findFirst()
                .orElse(null);
    }

    public String getRandomFactionName() {
        if (factions.isEmpty()) {
            return null;
        }

        List<String> factionNames = new ArrayList<>(factions.keySet());
        int randomIndex = ThreadLocalRandom.current().nextInt(factionNames.size());
        return factionNames.get(randomIndex);
    }

    public Collection<Faction> getFactions() {
        return factions.values();
    }
}
