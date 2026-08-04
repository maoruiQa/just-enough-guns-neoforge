package ttv.migami.jeg.client;

import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.SmokeParticle;
import ttv.migami.jeg.client.particle.BigExplosionParticle;
import ttv.migami.jeg.client.particle.CannonMuzzleFlareParticle;
import ttv.migami.jeg.client.particle.CasingParticle;
import ttv.migami.jeg.client.particle.CustomSmokeParticle;
import ttv.migami.jeg.client.particle.EntityLaserParticle;
import ttv.migami.jeg.client.particle.FlareSmokeParticle;
import ttv.migami.jeg.client.particle.GunMuzzleFlashParticle;
import ttv.migami.jeg.client.particle.LaserParticle;
import ttv.migami.jeg.client.particle.SmallExplosionParticle;
import ttv.migami.jeg.init.ModParticleTypes;

public final class ParticleFactoryRegistry {
    private ParticleFactoryRegistry() {}

    public static void init() {
        var registry = net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance();
        registry.register(ModParticleTypes.LASER.get(), LaserParticle.Provider::new);
        registry.register(ModParticleTypes.BIG_EXPLOSION.get(), BigExplosionParticle.Provider::new);
        registry.register(ModParticleTypes.SMALL_EXPLOSION.get(), SmallExplosionParticle.Provider::new);
        registry.register(ModParticleTypes.SMOKE.get(), SmokeParticle.Provider::new);
        registry.register(ModParticleTypes.FIRE.get(), FlameParticle.Provider::new);
        registry.register(ModParticleTypes.ENTITY_LASER.get(), EntityLaserParticle.Provider::new);

        registry.register(ModParticleTypes.FLARE_SMOKE.get(), FlareSmokeParticle.SmokeProvider::new);
        registry.register(ModParticleTypes.FLARE.get(), FlareSmokeParticle.RedProvider::new);
        registry.register(ModParticleTypes.BLUE_FLARE.get(), FlareSmokeParticle.BlueProvider::new);
        registry.register(ModParticleTypes.CUSTOM_SMOKE.get(), CustomSmokeParticle.Provider::new);

        registry.register(ModParticleTypes.FLAME.get(), FlameParticle.Provider::new);
        registry.register(ModParticleTypes.BLUE_FLAME.get(), FlameParticle.Provider::new);
        registry.register(ModParticleTypes.GUN_MUZZLE_FLASH.get(), GunMuzzleFlashParticle.Provider::new);
        registry.register(ModParticleTypes.CASING_PARTICLE.get(), CasingParticle.Provider::new);
        registry.register(ModParticleTypes.SHELL_PARTICLE.get(), CasingParticle.Provider::new);
        registry.register(ModParticleTypes.SPECTRE_CASING_PARTICLE.get(), CasingParticle.Provider::new);
        registry.register(ModParticleTypes.CANNON_MUZZLE_FLARE.get(), CannonMuzzleFlareParticle.Provider::new);
    }
}
