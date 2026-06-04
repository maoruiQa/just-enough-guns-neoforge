package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import ttv.migami.jeg.client.render.gun.GunAttachmentTransforms;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;

public final class GunScopeAttachmentLayer extends GeoRenderLayer<AnimatedGunItem> {
    private static final String ATTACHMENT_BONE = "attachment_bone";
    private static final String MODEL_ROOT = "geo/item/attachment/";
    private static final String TEXTURE_ROOT = "textures/animated/attachment/";
    private static final String PAINT_JOB_MODEL_ROOT = "geo/item/attachment/paintjob/";
    private static final String PAINT_JOB_TEXTURE_ROOT = "textures/animated/attachment/paintjob/";
    private static final double SCOPE_MODEL_Y_OFFSET = -3.0D / 16.0D;

    private final GunAttachmentGeoModel attachmentModel = new GunAttachmentGeoModel();

    public GunScopeAttachmentLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
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
        if (gunStack == null || gunStack.isEmpty() || !(gunStack.getItem() instanceof AnimatedGunItem gun)) {
            return;
        }
        ItemStack attachmentStack = GunAttachments.stack(gunStack, AttachmentType.SCOPE).orElse(ItemStack.EMPTY);
        if (!GunAttachments.canAttachStack(gunStack, AttachmentType.SCOPE, attachmentStack)) {
            return;
        }
        ResourceLocation attachmentId = GunAttachments.id(gunStack, AttachmentType.SCOPE).orElse(null);
        if (attachmentId == null) {
            return;
        }

        ResourceLocation model = model(attachmentId, gunStack);
        if (!exists(model)) {
            return;
        }

        ResourceLocation texture = texture(attachmentId, gunStack);
        BakedGeoModel bakedModel = this.attachmentModel.getBakedModel(model);
        RenderType attachmentRenderType = RenderType.entityTranslucent(texture);
        VertexConsumer attachmentBuffer = bufferSource.getBuffer(attachmentRenderType);

        poseStack.pushPose();
        GunAttachmentTransforms.transform(gun.getStats().id(), AttachmentType.SCOPE)
                .ifPresentOrElse(
                        transform -> transform.apply(poseStack),
                        () -> poseStack.translate(0.0D, SCOPE_MODEL_Y_OFFSET, 0.0D)
                );
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
        poseStack.popPose();
    }

    private static ResourceLocation model(ResourceLocation attachmentId, ItemStack gunStack) {
        String attachment = attachmentId.getPath();
        String paintJob = paintJob(gunStack);
        if (paintJob != null) {
            ResourceLocation painted = Reference.id(PAINT_JOB_MODEL_ROOT + paintJob + "/" + attachment + ".geo.json");
            if (exists(painted)) {
                return painted;
            }
        }
        return Reference.id(MODEL_ROOT + attachment + ".geo.json");
    }

    private static ResourceLocation texture(ResourceLocation attachmentId, ItemStack gunStack) {
        String attachment = attachmentId.getPath();
        String paintJob = paintJob(gunStack);
        if (paintJob != null) {
            ResourceLocation painted = Reference.id(PAINT_JOB_TEXTURE_ROOT + paintJob + "/" + attachment + ".png");
            if (exists(painted)) {
                return painted;
            }
        }

        ResourceLocation base = Reference.id(TEXTURE_ROOT + attachment + ".png");
        if (exists(base)) {
            return base;
        }
        return Reference.id(TEXTURE_ROOT + "combat_scope.png");
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
