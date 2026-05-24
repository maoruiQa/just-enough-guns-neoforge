package ttv.migami.jeg.vehicle.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.data.subdata.CollisionLevel;
import ttv.migami.jeg.vehicle.data.subdata.DamageModifierInfo;
import ttv.migami.jeg.vehicle.data.subdata.DestroyInfo;
import ttv.migami.jeg.vehicle.data.subdata.DismountInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineType;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeekInfo;
import ttv.migami.jeg.vehicle.data.subdata.TurretInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleContainerType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;

public final class VehicleDataManager {
    private static final Gson GSON = new Gson();
    private static volatile Map<ResourceLocation, VehicleData> data = defaultsOnly();
    private static volatile Map<ResourceLocation, String> syncedJson = Map.of();

    private VehicleDataManager() {}

    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Loader());
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

    public static Map<ResourceLocation, String> syncedJson() {
        return Map.copyOf(syncedJson);
    }

    public static void applySyncedJson(Map<ResourceLocation, String> objects) {
        Map<ResourceLocation, VehicleData> loaded = new HashMap<>(defaultsOnly());
        for (Map.Entry<ResourceLocation, String> entry : objects.entrySet()) {
            try {
                JsonObject object = JsonParser.parseString(entry.getValue()).getAsJsonObject();
                VehicleData vehicleData = parse(entry.getKey(), object);
                loaded.put(vehicleData.id(), vehicleData);
            } catch (RuntimeException exception) {
                JustEnoughGuns.LOGGER.error("Failed to apply synced vehicle data {}: {}", entry.getKey(), exception.getMessage());
            }
        }
        data = Map.copyOf(loaded);
    }

    private static Map<ResourceLocation, VehicleData> defaultsOnly() {
        return Map.of(
                DefaultVehicleData.TEST_WHEEL.id(), new VehicleData(DefaultVehicleData.TEST_WHEEL),
                DefaultVehicleData.LIGHT_COMBAT.id(), new VehicleData(DefaultVehicleData.LIGHT_COMBAT),
                DefaultVehicleData.TEST_HELICOPTER.id(), new VehicleData(DefaultVehicleData.TEST_HELICOPTER),
                DefaultVehicleData.TEST_BOAT.id(), new VehicleData(DefaultVehicleData.TEST_BOAT),
                DefaultVehicleData.TEST_ARTILLERY.id(), new VehicleData(DefaultVehicleData.TEST_ARTILLERY),
                DefaultVehicleData.TEST_AIRCRAFT.id(), new VehicleData(DefaultVehicleData.TEST_AIRCRAFT)
        );
    }

    private static final class Loader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
        private Loader() {
            super(GSON, "vehicles");
        }

        @Override
        public ResourceLocation getFabricId() {
            return Reference.id("vehicles");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, VehicleData> loaded = new HashMap<>(defaultsOnly());
            Map<ResourceLocation, String> raw = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                try {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    raw.put(entry.getKey(), GSON.toJson(object));
                    VehicleData vehicleData = parse(entry.getKey(), object);
                    loaded.put(vehicleData.id(), vehicleData);
                } catch (RuntimeException exception) {
                    JustEnoughGuns.LOGGER.error("Failed to load vehicle data {}: {}", entry.getKey(), exception.getMessage());
                }
            }
            data = Map.copyOf(loaded);
            syncedJson = Map.copyOf(raw);
            JustEnoughGuns.LOGGER.info("Loaded {} JEG vehicle data entries", data.size());
        }
    }

    private static VehicleData parse(ResourceLocation id, JsonObject object) {
        DefaultVehicleData fallback = fallbackFor(id);
        String entityType = getString(object, "entity_type", fallback.entityType());
        VehicleType vehicleType = getEnum(object, "vehicle_type", VehicleType.class, fallback.vehicleType());
        float maxHealth = getFloat(object, "max_health", fallback.maxHealth());
        float autoRepairPerTick = getFloat(object, "auto_repair_per_tick", fallback.autoRepairPerTick());
        int autoRepairCooldownTicks = getInt(object, "auto_repair_cooldown_ticks", fallback.autoRepairCooldownTicks());
        int maxEnergy = getInt(object, "max_energy", fallback.maxEnergy());
        float upStep = getFloat(object, "up_step", fallback.upStep());
        EngineInfo engine = parseEngine(getObject(object, "engine"), fallback.engine());
        List<SeatInfo> seats = parseSeats(object, fallback.seats());
        boolean allowFreeCam = getBoolean(object, "allow_free_cam", fallback.allowFreeCam());
        VehicleContainerType containerType = getEnum(object, "container_type", VehicleContainerType.class, fallback.containerType());
        CameraPos camera = parseCamera(getObject(object, "third_person_camera"), fallback.thirdPersonCamera());
        DismountInfo dismount = parseDismount(getObject(object, "dismount"), fallback.dismount());
        CollisionLevel collisionLevel = getEnum(object, "collision_level", CollisionLevel.class, fallback.collisionLevel());
        OBBInfo obb = parseObb(object, fallback.obb());
        VehicleArmorProfile armor = parseArmor(getObject(object, "armor"), fallback.armor());
        DamageModifierInfo damageModifier = parseDamageModifier(getObject(object, "damage_modifier"), fallback.damageModifier());
        List<VehicleWeaponInfo> weapons = parseWeapons(object, fallback.weapons());
        TurretInfo turret = parseTurret(getObject(object, "turret"), fallback.turret());
        boolean hasDecoy = getBoolean(object, "has_decoy", fallback.hasDecoy());
        SeekInfo seek = parseSeek(getObject(object, "seek"), fallback.seek());
        DestroyInfo destroy = parseDestroy(getObject(object, "destroy"), fallback.destroy());

        return new VehicleData(new DefaultVehicleData(
                id,
                entityType,
                vehicleType,
                maxHealth,
                autoRepairPerTick,
                autoRepairCooldownTicks,
                maxEnergy,
                upStep,
                engine,
                seats,
                allowFreeCam,
                containerType,
                camera,
                dismount,
                collisionLevel,
                obb,
                armor,
                damageModifier,
                weapons,
                turret,
                hasDecoy,
                seek,
                destroy
        ));
    }

    private static DefaultVehicleData fallbackFor(ResourceLocation id) {
        if (id.equals(DefaultVehicleData.LIGHT_COMBAT.id())) {
            return DefaultVehicleData.LIGHT_COMBAT;
        }
        if (id.equals(DefaultVehicleData.TEST_HELICOPTER.id())) {
            return DefaultVehicleData.TEST_HELICOPTER;
        }
        if (id.equals(DefaultVehicleData.TEST_BOAT.id())) {
            return DefaultVehicleData.TEST_BOAT;
        }
        if (id.equals(DefaultVehicleData.TEST_ARTILLERY.id())) {
            return DefaultVehicleData.TEST_ARTILLERY;
        }
        if (id.equals(DefaultVehicleData.TEST_AIRCRAFT.id())) {
            return DefaultVehicleData.TEST_AIRCRAFT;
        }
        return DefaultVehicleData.TEST_WHEEL;
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
                getDouble(object, "friction", fallback.friction()),
                getDouble(object, "steering_speed", fallback.steeringSpeed()),
                getInt(object, "energy_cost_rate", fallback.energyCostRate()),
                getDouble(object, "increment", fallback.increment()),
                getDouble(object, "decrement", fallback.decrement()),
                getDouble(object, "pitch_speed", fallback.pitchSpeed()),
                getDouble(object, "yaw_speed", fallback.yawSpeed()),
                getDouble(object, "roll_speed", fallback.rollSpeed()),
                getDouble(object, "lift_speed", fallback.liftSpeed()),
                getResourceLocation(object, "engine_start_sound", fallback.engineStartSound()),
                getFloat(object, "engine_sound_volume", fallback.engineSoundVolume())
        );
    }

    private static CameraPos parseCamera(JsonObject object, CameraPos fallback) {
        if (object == null) {
            return fallback;
        }
        return new CameraPos(
                getDouble(object, "x", fallback.x()),
                getDouble(object, "y", fallback.y()),
                getDouble(object, "z", fallback.z()),
                getDouble(object, "zoom_x", fallback.zoomX()),
                getDouble(object, "zoom_y", fallback.zoomY()),
                getDouble(object, "zoom_z", fallback.zoomZ()),
                getBoolean(object, "use_fixed_camera_pos", fallback.useFixedCameraPos()),
                getBoolean(object, "use_aircraft_camera", fallback.useAircraftCamera()),
                getDouble(object, "aircraft_x", fallback.aircraftX()),
                getDouble(object, "aircraft_y", fallback.aircraftY()),
                getDouble(object, "aircraft_z", fallback.aircraftZ()),
                getBoolean(object, "use_simulated_third_person", fallback.useSimulatedThirdPerson()),
                getDouble(object, "simulated_third_person_distance", fallback.simulatedThirdPersonDistance()),
                getDouble(object, "simulated_third_person_height", fallback.simulatedThirdPersonHeight()),
                getString(object, "transform", fallback.transform())
        );
    }

    private static DismountInfo parseDismount(JsonObject object, DismountInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new DismountInfo(
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
                    getBoolean(seat, "enclosed", fallbackSeat.enclosed()),
                    getBoolean(seat, "hide_passenger", fallbackSeat.hidePassenger()),
                    getBoolean(seat, "ban_hand", fallbackSeat.banHand()),
                    (float) getDouble(seat, "min_pitch", fallbackSeat.minPitch()),
                    (float) getDouble(seat, "max_pitch", fallbackSeat.maxPitch()),
                    (float) getDouble(seat, "min_yaw", fallbackSeat.minYaw()),
                    (float) getDouble(seat, "max_yaw", fallbackSeat.maxYaw()),
                    (float) getDouble(seat, "sensitivity_x", fallbackSeat.sensitivityX()),
                    (float) getDouble(seat, "sensitivity_y", fallbackSeat.sensitivityY()),
                    (float) getDouble(seat, "sensitivity_z", fallbackSeat.sensitivityZ()),
                    parseCamera(getObject(seat, "zoom_camera"), fallbackSeat.zoomCamera()),
                    parseDismount(getObject(seat, "dismount"), fallbackSeat.dismount())
            ));
        }
        seats.sort(Comparator.comparingInt(SeatInfo::index));
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
            JsonObject muzzle = getObject(weapon, "muzzle");
            weapons.add(new VehicleWeaponInfo(
                    ResourceLocation.parse(getString(weapon, "weapon", "jeg:assault_rifle")),
                    ResourceLocation.parse(getString(weapon, "ammo", "jeg:rifle_ammo")),
                    getInt(weapon, "energy_cost", 0),
                    getBoolean(weapon, "guided", false),
                    getInt(weapon, "seat", getInt(weapon, "seat_index", -1)),
                    muzzle == null ? Double.NaN : getDouble(muzzle, "x", Double.NaN),
                    muzzle == null ? Double.NaN : getDouble(muzzle, "y", Double.NaN),
                    muzzle == null ? Double.NaN : getDouble(muzzle, "z", Double.NaN)
            ));
        }
        return List.copyOf(weapons);
    }

    private static TurretInfo parseTurret(JsonObject object, TurretInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new TurretInfo(
                getInt(object, "seat", fallback.seatIndex()),
                getDouble(object, "render_pivot_y", fallback.renderPivotY()),
                getDouble(object, "origin_x", fallback.originX()),
                getDouble(object, "origin_y", fallback.originY()),
                getDouble(object, "origin_z", fallback.originZ()),
                getDouble(object, "barrel_x", fallback.barrelX()),
                getDouble(object, "barrel_y", fallback.barrelY()),
                getDouble(object, "barrel_z", fallback.barrelZ()),
                getBoolean(object, "guided_uses_turret", fallback.guidedUsesTurret())
        );
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

    private static SeekInfo parseSeek(JsonObject object, SeekInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new SeekInfo(
                getDouble(object, "range", fallback.range()),
                getDouble(object, "angle", fallback.angle()),
                getBoolean(object, "warns_target", fallback.warnsTarget())
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

    private static DamageModifierInfo parseDamageModifier(JsonObject object, DamageModifierInfo fallback) {
        if (object == null) {
            return fallback;
        }
        return new DamageModifierInfo(getFloat(object, "global_multiplier", fallback.globalMultiplier()));
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

    private static ResourceLocation getResourceLocation(JsonObject object, String name, ResourceLocation fallback) {
        JsonElement element = object.get(name);
        return element != null ? ResourceLocation.parse(element.getAsString()) : fallback;
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
