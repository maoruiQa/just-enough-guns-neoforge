package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerGeoRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomGeoRenderer;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.MolotovCocktailEntity;
import ttv.migami.jeg.entity.SmokeGrenadeEntity;
import ttv.migami.jeg.entity.StunGrenadeEntity;
import ttv.migami.jeg.entity.WaterBombEntity;
import ttv.migami.jeg.entity.monster.Ghoul;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.client.render.TestVehicleRenderer;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FallbackClientRenderers {
    private FallbackClientRenderers() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GHOUL.get(), FallbackGhoulRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<GrenadeEntity>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.STUN_GRENADE.get(), context -> new ThrownItemRenderer<StunGrenadeEntity>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.SMOKE_GRENADE.get(), context -> new ThrownItemRenderer<SmokeGrenadeEntity>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.MOLOTOV_COCKTAIL.get(), context -> new ThrownItemRenderer<MolotovCocktailEntity>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.WATER_BOMB.get(), context -> new ThrownItemRenderer<WaterBombEntity>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerGeoRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerGeoRenderer::new);
        event.registerEntityRenderer(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomGeoRenderer::new);
        event.registerEntityRenderer(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomGeoRenderer::new);
        event.registerEntityRenderer(ModEntities.RAID_ENTITY.get(), NullEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.TEST_WHEEL_VEHICLE.get(), TestVehicleRenderer::new);
        event.registerEntityRenderer(ModEntities.VEHICLE_DECOY.get(), NullEntityRenderer::new);
    }

    private static final class FallbackGhoulRenderer extends MobRenderer<Ghoul, ZombieModel<Ghoul>> {
        private static final ResourceLocation TEXTURE = Reference.id("textures/entity/zombie/ghoul.png");

        private FallbackGhoulRenderer(EntityRendererProvider.Context context) {
            super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(Ghoul entity) {
            return TEXTURE;
        }

        @Override
        protected int getBlockLightLevel(Ghoul entity, net.minecraft.core.BlockPos pos) {
            return 7;
        }
    }

    private static final class NullEntityRenderer<T extends Entity> extends EntityRenderer<T> {
        private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

        private NullEntityRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return EMPTY_TEXTURE;
        }

        @Override
        public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            // Intentionally empty fallback to avoid null renderer crashes.
        }
    }
}
