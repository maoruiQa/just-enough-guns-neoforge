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
import ttv.migami.jeg.entity.GunnerEntity;
import ttv.migami.jeg.entity.monster.Ghoul;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> GUNNER = REGISTER.register(
            "gunner",
            () -> {
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "gunner"));
                return EntityType.Builder.of(GunnerEntity::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .build(key);
            }
    );

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
}
