package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
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
import ttv.migami.jeg.vehicle.entity.LightCombatVehicleEntity;
import ttv.migami.jeg.vehicle.entity.TestHelicopterEntity;
import ttv.migami.jeg.vehicle.entity.TestWheelVehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Ghoul>> GHOUL = REGISTER.register(
            "ghoul",
            () -> EntityType.Builder.of(Ghoul::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build("ghoul")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET = REGISTER.register(
            "bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("bullet")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeEntity>> GRENADE = REGISTER.register(
            "grenade",
            () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("grenade")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<StunGrenadeEntity>> STUN_GRENADE = REGISTER.register(
            "stun_grenade",
            () -> EntityType.Builder.<StunGrenadeEntity>of(StunGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("stun_grenade")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeGrenadeEntity>> SMOKE_GRENADE = REGISTER.register(
            "smoke_grenade",
            () -> EntityType.Builder.<SmokeGrenadeEntity>of(SmokeGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("smoke_grenade")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<MolotovCocktailEntity>> MOLOTOV_COCKTAIL = REGISTER.register(
            "molotov_cocktail",
            () -> EntityType.Builder.<MolotovCocktailEntity>of(MolotovCocktailEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("molotov_cocktail")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<WaterBombEntity>> WATER_BOMB = REGISTER.register(
            "water_bomb",
            () -> EntityType.Builder.<WaterBombEntity>of(WaterBombEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("water_bomb")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PhantomGunner>> PHANTOM_GUNNER = REGISTER.register(
            "phantom_gunner",
            () -> EntityType.Builder.of(PhantomGunner::new, MobCategory.MONSTER)
                    .sized(4.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("phantom_gunner")
    );

    // Summoned by Terror Phantom / Bound Terror Phantom: identical to Phantom Gunner except half max health.
    public static final DeferredHolder<EntityType<?>, EntityType<PhantomGunnerMinion>> PHANTOM_GUNNER_MINION = REGISTER.register(
            "phantom_gunner_minion",
            () -> EntityType.Builder.of(PhantomGunnerMinion::new, MobCategory.MONSTER)
                    .sized(4.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("phantom_gunner_minion")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TerrorPhantom>> TERROR_PHANTOM = REGISTER.register(
            "terror_phantom",
            () -> EntityType.Builder.of(TerrorPhantom::new, MobCategory.MONSTER)
                    .sized(40.19531F, 1.5F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("terror_phantom")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TerrorPhantomGuardian>> TERROR_PHANTOM_GUARDIAN = REGISTER.register(
            "terror_phantom_guardian",
            () -> EntityType.Builder.of(TerrorPhantomGuardian::new, MobCategory.MONSTER)
                    .sized(45.21973F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("terror_phantom_guardian")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<RaidEntity>> RAID_ENTITY = REGISTER.register(
            "raid_entity",
            () -> EntityType.Builder.<RaidEntity>of(RaidEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .fireImmune()
                    .build("raid_entity")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestWheelVehicleEntity>> TEST_WHEEL_VEHICLE = REGISTER.register(
            "test_wheel_vehicle",
            () -> EntityType.Builder.<TestWheelVehicleEntity>of(TestWheelVehicleEntity::new, MobCategory.MISC)
                    .sized(1.4F, 1.1F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("test_wheel_vehicle")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<LightCombatVehicleEntity>> LIGHT_COMBAT_VEHICLE = REGISTER.register(
            "light_combat_vehicle",
            () -> EntityType.Builder.<LightCombatVehicleEntity>of(LightCombatVehicleEntity::new, MobCategory.MISC)
                    .sized(1.8F, 1.35F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("light_combat_vehicle")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<TestHelicopterEntity>> TEST_HELICOPTER = REGISTER.register(
            "test_helicopter",
            () -> EntityType.Builder.<TestHelicopterEntity>of(TestHelicopterEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("test_helicopter")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<VehicleDecoyEntity>> VEHICLE_DECOY = REGISTER.register(
            "vehicle_decoy",
            () -> EntityType.Builder.<VehicleDecoyEntity>of(VehicleDecoyEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("vehicle_decoy")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<VehicleMissileEntity>> VEHICLE_MISSILE = REGISTER.register(
            "vehicle_missile",
            () -> EntityType.Builder.<VehicleMissileEntity>of(VehicleMissileEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("vehicle_missile")
    );
}

