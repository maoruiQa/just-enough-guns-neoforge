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
import ttv.migami.jeg.client.particle.ConfettiParticle;
import ttv.migami.jeg.client.particle.FlareSmokeParticle;
import ttv.migami.jeg.client.particle.GunMuzzleFlashParticle;
import ttv.migami.jeg.client.particle.HitMarkerParticle;
import ttv.migami.jeg.client.particle.LaserParticle;
import ttv.migami.jeg.client.particle.PopcornParticle;
import ttv.migami.jeg.client.particle.SmallExplosionParticle;
import ttv.migami.jeg.client.particle.SonicRingParticle;
import ttv.migami.jeg.init.ModParticleTypes;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ParticleFactoryRegistry {
    private ParticleFactoryRegistry() {}

    @SubscribeEvent
    public static void onRegisterParticleFactory(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ModParticleTypes.LASER.get(), (option, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new LaserParticle(level, x, y, z, option.direction(), option.pos()));
        event.registerSpriteSet(ModParticleTypes.BIG_EXPLOSION.get(), BigExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SMALL_EXPLOSION.get(), SmallExplosionParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SMOKE.get(), SmokeParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.FIRE.get(), FlameParticle.Provider::new);

        event.registerSpriteSet(ModParticleTypes.FLARE_SMOKE.get(), FlareSmokeParticle.SmokeProvider::new);
        event.registerSpriteSet(ModParticleTypes.FLARE.get(), FlareSmokeParticle.RedProvider::new);
        event.registerSpriteSet(ModParticleTypes.BLUE_FLARE.get(), FlareSmokeParticle.BlueProvider::new);

        event.registerSpriteSet(ModParticleTypes.FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BLUE_FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.GUN_MUZZLE_FLASH.get(), GunMuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SONIC_RING.get(), SonicRingParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.BIG_SONIC_RING.get(), SonicRingParticle.BigProvider::new);
        event.registerSpriteSet(ModParticleTypes.CONFETTI.get(), ConfettiParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.HIT_MARKER.get(), HitMarkerParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.POPCORN.get(), PopcornParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CANNON_MUZZLE_FLARE.get(), CannonMuzzleFlareParticle.Provider::new);
    }
}
