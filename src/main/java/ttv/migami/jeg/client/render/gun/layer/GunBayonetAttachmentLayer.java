package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunAttachmentGeoModel;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;

public final class GunBayonetAttachmentLayer extends GeoRenderLayer<AnimatedGunItem> {
    private static final String ATTACHMENT_BONE = "attachment_bone";
    private static final String MODEL_ROOT = "geo/item/attachment/";
    private static final String TEXTURE_ROOT = "textures/animated/attachment/";
    private static final String PAINT_JOB_TEXTURE_ROOT = "textures/animated/attachment/paintjob/";
    private static final Map<ResourceLocation, String> VANILLA_SWORD_ATTACHMENTS = Map.of(
            ResourceLocation.withDefaultNamespace("wooden_sword"), "wooden_sword",
            ResourceLocation.withDefaultNamespace("stone_sword"), "stone_sword",
            ResourceLocation.withDefaultNamespace("iron_sword"), "iron_sword",
            ResourceLocation.withDefaultNamespace("golden_sword"), "golden_sword",
            ResourceLocation.withDefaultNamespace("diamond_sword"), "diamond_sword",
            ResourceLocation.withDefaultNamespace("netherite_sword"), "netherite_sword"
    );

    private final GunAttachmentGeoModel attachmentModel = new GunAttachmentGeoModel();

    public GunBayonetAttachmentLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(
            PoseStack poseStack,
            AnimatedGunItem animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!ATTACHMENT_BONE.equals(bone.getName()) || !(getRenderer() instanceof AnimatedGunRenderer renderer)) {
            return;
        }

        ItemStack gunStack = renderer.getCurrentItemStack();
        if (gunStack == null || gunStack.isEmpty()) {
            return;
        }

        ItemStack attachmentStack = GunAttachments.stack(gunStack, AttachmentType.BARREL).orElse(ItemStack.EMPTY);
        if (!GunAttachments.canAttachStack(gunStack, AttachmentType.BARREL, attachmentStack)) {
            return;
        }

        String attachmentName = attachmentName(attachmentStack);
        if (attachmentName == null) {
            return;
        }

        ResourceLocation model = Reference.id(MODEL_ROOT + attachmentName + ".geo.json");
        if (!exists(model)) {
            return;
        }

        ResourceLocation texture = texture(attachmentName, gunStack);
        if (!exists(texture)) {
            return;
        }
        BakedGeoModel bakedModel = this.attachmentModel.getBakedModel(model);
        RenderType attachmentRenderType = RenderType.entityTranslucent(texture);
        VertexConsumer attachmentBuffer = bufferSource.getBuffer(attachmentRenderType);

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                attachmentRenderType,
                attachmentBuffer,
                partialTick,
                packedLight,
                packedOverlay,
                0xFFFFFFFF
        );
    }

    private static String attachmentName(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? null : VANILLA_SWORD_ATTACHMENTS.get(id);
    }

    private static ResourceLocation texture(String attachmentName, ItemStack gunStack) {
        String paintJob = paintJob(gunStack);
        if (paintJob != null) {
            ResourceLocation painted = Reference.id(PAINT_JOB_TEXTURE_ROOT + paintJob + "/" + attachmentName + ".png");
            if (exists(painted)) {
                return painted;
            }
        }

        return Reference.id(TEXTURE_ROOT + attachmentName + ".png");
    }

    private static String paintJob(ItemStack gunStack) {
        Item item = GunAttachments.cosmeticItem(gunStack, AttachmentType.PAINT_JOB).orElse(null);
        if (!(item instanceof PaintJobCanItem paintJobCanItem)) {
            return null;
        }

        String paintJob = paintJobCanItem.paintJob();
        return paintJob == null || paintJob.isBlank() ? null : paintJob;
    }

    private static boolean exists(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
