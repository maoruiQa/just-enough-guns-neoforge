package ttv.migami.jeg.vehicle.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.data.subdata.DestroyInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineType;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeekInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleContainerType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;

public final class VehicleDataManager {
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, VehicleData> data = defaultsOnly();

    private VehicleDataManager() {}

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static VehicleData get(ResourceLocation id) {
        return data.getOrDefault(id, data.get(DefaultVehicleData.TEST_WHEEL.id()));
    }

    public static VehicleData testVehicle() {
        return get(DefaultVehicleData.TEST_WHEEL.id());
    }

    public static Map<ResourceLocation, VehicleData> all() {
        return Map.copyOf(data);
    }

    private static Map<ResourceLocation, VehicleData> defaultsOnly() {
        return Map.of(
                DefaultVehicleData.TEST_WHEEL.id(), new VehicleData(DefaultVehicleData.TEST_WHEEL),
                DefaultVehicleData.LIGHT_COMBAT.id(), new VehicleData(DefaultVehicleData.LIGHT_COMBAT),
                DefaultVehicleData.TEST_HELICOPTER.id(), new VehicleData(DefaultVehicleData.TEST_HELICOPTER),
                DefaultVehicleData.TEST_BOAT.id(), new VehicleData(DefaultVehicleData.TEST_BOAT),
                DefaultVehicleData.TEST_ARTILLERY.id(), new VehicleData(DefaultVehicleData.TEST_ARTILLERY)
        );
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() {
            super(GSON, "vehicles");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, VehicleData> loaded = new HashMap<>(defaultsOnly());
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                try {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    VehicleData vehicleData = parse(entry.getKey(), object);
                    loaded.put(vehicleData.id(), vehicleData);
                } catch (RuntimeException exception) {
                    JustEnoughGuns.LOGGER.error("Failed to load vehicle data {}: {}", entry.getKey(), exception.getMessage());
                }
            }
            data = Map.copyOf(loaded);
            JustEnoughGuns.LOGGER.info("Loaded {} JEG vehicle data entries", data.size());
        }
    }

    private static VehicleData parse(ResourceLocation id, JsonObject object) {
        DefaultVehicleData fallback = DefaultVehicleData.TEST_WHEEL;
        String entityType = getString(object, "entity_type", fallback.entityType());
        VehicleType vehicleType = getEnum(object, "vehicle_type", VehicleType.class, fallback.vehicleType());
        float maxHealth = getFloat(object, "max_health", fallback.maxHealth());
        float autoRepairPerTick = getFloat(object, "auto_repair_per_tick", fallback.autoRepairPerTick());
        int autoRepairCooldownTicks = getInt(object, "auto_repair_cooldown_ticks", fallback.autoRepairCooldownTicks());
        int maxEnergy = getInt(object, "max_energy", fallback.maxEnergy());
        EngineInfo engine = parseEngine(getObject(object, "engine"), fallback.engine());
        List<SeatInfo> seats = parseSeats(object, fallback.seats());
        boolean allowFreeCam = getBoolean(object, "allow_free_cam", fallback.allowFreeCam());
        VehicleContainerType containerType = getEnum(object, "container_type", VehicleContainerType.class, fallback.containerType());
        CameraPos camera = parseCamera(getObject(object, "third_person_camera"), fallback.thirdPersonCamera());
        OBBInfo obb = parseObb(object, fallback.obb());
        VehicleArmorProfile armor = parseArmor(getObject(object, "armor"), fallback.armor());
        List<VehicleWeaponInfo> weapons = parseWeapons(object, fallback.weapons());
        DestroyInfo destroy = parseDestroy(getObject(object, "destroy"), fallback.destroy());

        return new VehicleData(new DefaultVehicleData(
                id,
                entityType,
                vehicleType,
                maxHealth,
                autoRepairPerTick,
                autoRepairCooldownTicks,
                maxEnergy,
                engine,
                seats,
                allowFreeCam,
                containerType,
                camera,
                obb,
                armor,
                weapons,
                SeekInfo.NONE,
                destroy
        ));
    }

    private static EngineInfo parseEngine(JsonObject object, EngineInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new EngineInfo(
                getEnum(object, "type", EngineType.class, fallback.type()),
                getDouble(object, "acceleration", fallback.acceleration()),
                getDouble(object, "max_forward_speed", fallback.maxForwardSpeed()),
                getDouble(object, "max_reverse_speed", fallback.maxReverseSpeed()),
                getDouble(object, "friction", fallback.friction())
        );
    }

    private static CameraPos parseCamera(JsonObject object, CameraPos fallback) {
        if (object == null) {
            return fallback;
        }
        return new CameraPos(
                getDouble(object, "x", fallback.x()),
                getDouble(object, "y", fallback.y()),
                getDouble(object, "z", fallback.z())
        );
    }

    private static List<SeatInfo> parseSeats(JsonObject object, List<SeatInfo> fallback) {
        JsonElement element = object.get("seats");
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            return fallback;
        }
        java.util.ArrayList<SeatInfo> seats = new java.util.ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonObject seat = array.get(index).getAsJsonObject();
            SeatInfo fallbackSeat = index < fallback.size() ? fallback.get(index) : SeatInfo.DRIVER;
            seats.add(new SeatInfo(
                    getInt(seat, "index", index),
                    getDouble(seat, "x", fallbackSeat.x()),
                    getDouble(seat, "y", fallbackSeat.y()),
                    getDouble(seat, "z", fallbackSeat.z()),
                    getBoolean(seat, "driver", index == 0),
                    getBoolean(seat, "enclosed", fallbackSeat.enclosed())
            ));
        }
        return List.copyOf(seats);
    }

    private static OBBInfo parseObb(JsonObject object, OBBInfo fallback) {
        JsonElement element = object.get("obb");
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            return fallback;
        }
        java.util.ArrayList<OBBInfo.Box> boxes = new java.util.ArrayList<>();
        for (JsonElement boxElement : array) {
            JsonObject box = boxElement.getAsJsonObject();
            OBBInfo.Part part = getEnum(box, "part", OBBInfo.Part.class, OBBInfo.Part.BODY);
            boxes.add(new OBBInfo.Box(
                    part,
                    getDouble(box, "x", 0.0D),
                    getDouble(box, "y", 0.75D),
                    getDouble(box, "z", 0.0D),
                    getDouble(box, "half_width", 0.5D),
                    getDouble(box, "half_height", 0.5D),
                    getDouble(box, "half_depth", 0.5D)
            ));
        }
        return new OBBInfo(List.copyOf(boxes));
    }

    private static List<VehicleWeaponInfo> parseWeapons(JsonObject object, List<VehicleWeaponInfo> fallback) {
        JsonElement element = object.get("weapons");
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            return fallback;
        }
        java.util.ArrayList<VehicleWeaponInfo> weapons = new java.util.ArrayList<>();
        for (JsonElement weaponElement : array) {
            JsonObject weapon = weaponElement.getAsJsonObject();
            weapons.add(new VehicleWeaponInfo(
                    ResourceLocation.parse(getString(weapon, "weapon", "jeg:assault_rifle")),
                    ResourceLocation.parse(getString(weapon, "ammo", "jeg:rifle_ammo")),
                    getInt(weapon, "energy_cost", 0),
                    getBoolean(weapon, "guided", false)
            ));
        }
        return List.copyOf(weapons);
    }

    private static DestroyInfo parseDestroy(JsonObject object, DestroyInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new DestroyInfo(
                getBoolean(object, "explodes", fallback.explodes()),
                getFloat(object, "explosion_power", fallback.explosionPower())
        );
    }

    private static VehicleArmorProfile parseArmor(JsonObject object, VehicleArmorProfile fallback) {
        if (object == null) {
            return fallback;
        }
        VehiclePartArmorProfile fallbackPart = parseArmorPart(getObject(object, "fallback"), fallback.fallback());
        EnumMap<OBBInfo.Part, VehiclePartArmorProfile> parts = new EnumMap<>(OBBInfo.Part.class);
        JsonObject partsObject = getObject(object, "parts");
        if (partsObject != null) {
            for (Map.Entry<String, JsonElement> entry : partsObject.entrySet()) {
                OBBInfo.Part part = parseEnumValue(entry.getKey(), OBBInfo.Part.class);
                parts.put(part, parseArmorPart(entry.getValue().getAsJsonObject(), fallbackPart));
            }
        }
        return new VehicleArmorProfile(fallbackPart, parts);
    }

    private static VehiclePartArmorProfile parseArmorPart(JsonObject object, VehiclePartArmorProfile fallback) {
        if (object == null) {
            return fallback;
        }
        return new VehiclePartArmorProfile(
                getFloat(object, "rating", fallback.rating()),
                getFloat(object, "undermatch_multiplier", fallback.undermatchMultiplier()),
                getFloat(object, "overmatch_multiplier", fallback.overmatchMultiplier()),
                getFloat(object, "passenger_leak_multiplier", fallback.passengerLeakMultiplier()),
                getFloat(object, "part_damage_multiplier", fallback.partDamageMultiplier())
        );
    }

    private static JsonObject getObject(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String getString(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        return element != null ? element.getAsString() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String name, boolean fallback) {
        JsonElement element = object.get(name);
        return element != null ? element.getAsBoolean() : fallback;
    }

    private static int getInt(JsonObject object, String name, int fallback) {
        JsonElement element = object.get(name);
        return element != null ? element.getAsInt() : fallback;
    }

    private static float getFloat(JsonObject object, String name, float fallback) {
        JsonElement element = object.get(name);
        return element != null ? element.getAsFloat() : fallback;
    }

    private static double getDouble(JsonObject object, String name, double fallback) {
        JsonElement element = object.get(name);
        return element != null ? element.getAsDouble() : fallback;
    }

    private static <T extends Enum<T>> T getEnum(JsonObject object, String name, Class<T> type, T fallback) {
        JsonElement element = object.get(name);
        return element != null ? parseEnumValue(element.getAsString(), type) : fallback;
    }

    private static <T extends Enum<T>> T parseEnumValue(String value, Class<T> type) {
        try {
            return Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonSyntaxException("Unknown " + type.getSimpleName() + " value: " + value, exception);
        }
    }
}
