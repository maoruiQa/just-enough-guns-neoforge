package ttv.migami.jeg.client;

import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.SmokeParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.particle.BigExplosionParticle;
import ttv.migami.jeg.client.particle.CannonMuzzleFlareParticle;
import ttv.migami.jeg.client.particle.CasingParticle;
import ttv.migami.jeg.client.particle.EntityLaserParticle;
import ttv.migami.jeg.client.particle.FlareSmokeParticle;
import ttv.migami.jeg.client.particle.GunMuzzleFlashParticle;
import ttv.migami.jeg.client.particle.LaserParticle;
import ttv.migami.jeg.client.particle.SmallExplosionParticle;
import ttv.migami.jeg.init.ModParticleTypes;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ParticleFactoryRegistry {
    private ParticleFactoryRegistry() {}

    @SubscribeEvent
    public static void onRegisterParticleFactory(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticleTypes.LASER.get(), LaserParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BIG_EXPLOSION.get(), BigExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SMALL_EXPLOSION.get(), SmallExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SMOKE.get(), SmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.FIRE.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ENTITY_LASER.get(), EntityLaserParticle.Provider::new);

        event.registerSpriteSet(ModParticleTypes.FLARE_SMOKE.get(), FlareSmokeParticle.SmokeProvider::new);
        event.registerSpriteSet(ModParticleTypes.FLARE.get(), FlareSmokeParticle.RedProvider::new);
        event.registerSpriteSet(ModParticleTypes.BLUE_FLARE.get(), FlareSmokeParticle.BlueProvider::new);
        event.registerSpriteSet(ModParticleTypes.COLORED_FLARE_SMOKE.get(), FlareSmokeParticle.ColoredProvider::new);

        event.registerSpriteSet(ModParticleTypes.FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BLUE_FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.GUN_MUZZLE_FLASH.get(), GunMuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SHELL_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SPECTRE_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CANNON_MUZZLE_FLARE.get(), CannonMuzzleFlareParticle.Provider::new);
    }
}
