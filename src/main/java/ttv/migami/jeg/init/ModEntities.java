package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.entity.MolotovCocktailEntity;
import ttv.migami.jeg.entity.SmokeGrenadeEntity;
import ttv.migami.jeg.entity.StunGrenadeEntity;
import ttv.migami.jeg.entity.WaterBombEntity;
import ttv.migami.jeg.entity.monster.Ghoul;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantomGuardian;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunnerMinion;
import ttv.migami.jeg.faction.raid.RaidEntity;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;
import ttv.migami.jeg.vehicle.entity.LightCombatVehicleEntity;
import ttv.migami.jeg.vehicle.entity.TestAircraftEntity;
import ttv.migami.jeg.vehicle.entity.TestArtilleryEntity;
import ttv.migami.jeg.vehicle.entity.TestBoatEntity;
import ttv.migami.jeg.vehicle.entity.TestHelicopterEntity;
import ttv.migami.jeg.vehicle.entity.TestWheelVehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Ghoul>> GHOUL = REGISTER.register(
            "ghoul",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("ghoul"));
                return EntityType.Builder.of(Ghoul::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET = REGISTER.register(
            "bullet",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("bullet"));
                return EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(4)
                        .updateInterval(1)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeEntity>> GRENADE = REGISTER.register(
            "grenade",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("grenade"));
                return EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(6)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<StunGrenadeEntity>> STUN_GRENADE = REGISTER.register(
            "stun_grenade",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("stun_grenade"));
                return EntityType.Builder.<StunGrenadeEntity>of(StunGrenadeEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(6)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE = REGISTER.register(
            "smoke_grenade",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("smoke_grenade"));
                return EntityType.Builder.<SmokeGrenadeEntity>of(SmokeGrenadeEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(6)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<MolotovCocktailEntity>> MOLOTOV_COCKTAIL = REGISTER.register(
            "molotov_cocktail",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("molotov_cocktail"));
                return EntityType.Builder.<MolotovCocktailEntity>of(MolotovCocktailEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(6)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<WaterBombEntity>> WATER_BOMB = REGISTER.register(
            "water_bomb",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("water_bomb"));
                return EntityType.Builder.<WaterBombEntity>of(WaterBombEntity::new, MobCategory.MISC)
                        .sized(0.25F, 0.25F)
                        .clientTrackingRange(6)
                        .updateInterval(2)
                        .build(key);
            }
    );


    public static final DeferredHolder<EntityType<?>, EntityType<PlacedExplosiveEntity>> PLACED_EXPLOSIVE = REGISTER.register(
            "placed_explosive",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("placed_explosive"));
                return EntityType.Builder.<PlacedExplosiveEntity>of(PlacedExplosiveEntity::new, MobCategory.MISC)
                        .sized(0.5F, 0.25F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = REGISTER.register(
            "drone",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("drone"));
                return EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                        .sized(0.6F, 0.35F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomGunner>> PHANTOM_GUNNER = REGISTER.register(
            "phantom_gunner",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("phantom_gunner"));
                return EntityType.Builder.of(PhantomGunner::new, MobCategory.MONSTER)
                        .sized(4.0F, 1.0F)
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomGunnerMinion>> PHANTOM_GUNNER_MINION = REGISTER.register(
            "phantom_gunner_minion",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("phantom_gunner_minion"));
                return EntityType.Builder.of(PhantomGunnerMinion::new, MobCategory.MONSTER)
                        .sized(4.0F, 1.0F)
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TerrorPhantom>> TERROR_PHANTOM = REGISTER.register(
            "terror_phantom",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("terror_phantom"));
                return EntityType.Builder.of(TerrorPhantom::new, MobCategory.MONSTER)
                        .sized(40.19531F, 1.5F)
                        .clientTrackingRange(14)
                        .updateInterval(2)
                        .fireImmune()
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TerrorPhantomGuardian>> TERROR_PHANTOM_GUARDIAN = REGISTER.register(
            "terror_phantom_guardian",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("terror_phantom_guardian"));
                return EntityType.Builder.of(TerrorPhantomGuardian::new, MobCategory.MONSTER)
                        .sized(45.21973F, 1.8F)
                        .clientTrackingRange(14)
                        .updateInterval(2)
                        .fireImmune()
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<RaidEntity>> RAID_ENTITY = REGISTER.register(
            "raid_entity",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Reference.id("raid_entity"));
                return EntityType.Builder.<RaidEntity>of(RaidEntity::new, MobCategory.MISC)
                        .sized(3.0F, 3.0F)
                        .clientTrackingRange(8)
                        .updateInterval(3)
                        .fireImmune()
                        .build(key);
            }
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestWheelVehicleEntity>> TEST_WHEEL_VEHICLE = REGISTER.register(
            "test_wheel_vehicle",
            () -> EntityType.Builder.<TestWheelVehicleEntity>of(TestWheelVehicleEntity::new, MobCategory.MISC)
                    .sized(1.4F, 1.1F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("test_wheel_vehicle"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<LightCombatVehicleEntity>> LIGHT_COMBAT_VEHICLE = REGISTER.register(
            "light_combat_vehicle",
            () -> EntityType.Builder.<LightCombatVehicleEntity>of(LightCombatVehicleEntity::new, MobCategory.MISC)
                    .sized(1.8F, 1.35F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("light_combat_vehicle"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestHelicopterEntity>> TEST_HELICOPTER = REGISTER.register(
            "test_helicopter",
            () -> EntityType.Builder.<TestHelicopterEntity>of(TestHelicopterEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.25F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("test_helicopter"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestBoatEntity>> TEST_BOAT = REGISTER.register(
            "test_boat",
            () -> EntityType.Builder.<TestBoatEntity>of(TestBoatEntity::new, MobCategory.MISC)
                    .sized(1.7F, 0.75F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("test_boat"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestArtilleryEntity>> TEST_ARTILLERY = REGISTER.register(
            "test_artillery",
            () -> EntityType.Builder.<TestArtilleryEntity>of(TestArtilleryEntity::new, MobCategory.MISC)
                    .sized(1.3F, 1.45F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("test_artillery"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestAircraftEntity>> TEST_AIRCRAFT = REGISTER.register(
            "test_aircraft",
            () -> EntityType.Builder.<TestAircraftEntity>of(TestAircraftEntity::new, MobCategory.MISC)
                    .sized(2.4F, 0.9F)
                    .clientTrackingRange(14)
                    .updateInterval(1)
                    .build(entityKey("test_aircraft"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> TRUCK = configuredVehicle("truck", 2.6F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> LAV150 = configuredVehicle("lav150", 2.8F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> SPEEDBOAT = configuredVehicle("speedboat", 3.0F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> AH6 = configuredVehicle("ah6", 3.0F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> A10 = configuredVehicle("a10", 2.8F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> BMP2 = configuredVehicle("bmp2", 4.4F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> MI28 = configuredVehicle("mi28", 4.5F, 4.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> TOM6 = configuredVehicle("tom6", 2.8F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> LASER_TOWER = configuredVehicle("laser_tower", 1.4F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> HPJ11 = configuredVehicle("hpj11", 1.5F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> WAVEFORCE_TOWER = configuredVehicle("waveforce_tower", 1.5F, 2.1F);

    public static final DeferredHolder<EntityType<?>, EntityType<VehicleDecoyEntity>> VEHICLE_DECOY = REGISTER.register(
            "vehicle_decoy",
            () -> EntityType.Builder.<VehicleDecoyEntity>of(VehicleDecoyEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(entityKey("vehicle_decoy"))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<VehicleMissileEntity>> VEHICLE_MISSILE = REGISTER.register(
            "vehicle_missile",
            () -> EntityType.Builder.<VehicleMissileEntity>of(VehicleMissileEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(entityKey("vehicle_missile"))
    );

    private static DeferredHolder<EntityType<?>, EntityType<ConfiguredVehicleEntity>> configuredVehicle(String path, float width, float height) {
        return REGISTER.register(
                path,
                () -> EntityType.Builder.<ConfiguredVehicleEntity>of((type, level) -> new ConfiguredVehicleEntity(type, level, Reference.id(path)), MobCategory.MISC)
                        .sized(width, height)
                        .clientTrackingRange(14)
                        .updateInterval(1)
                        .build(entityKey(path))
        );
    }

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Reference.id(path));
    }
}
