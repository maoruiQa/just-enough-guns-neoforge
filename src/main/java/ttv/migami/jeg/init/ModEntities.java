package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.monster.Ghoul;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantomGuardian;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunnerMinion;

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

    // Summoned by Terror Phantom / Bound Terror Phantom: identical to Phantom Gunner except reduced max health.
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
                        .sized(4.0F, 1.5F)
                        .clientTrackingRange(8)
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
                        .sized(4.5F, 1.8F)
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .fireImmune()
                        .build(key);
            }
    );
}
