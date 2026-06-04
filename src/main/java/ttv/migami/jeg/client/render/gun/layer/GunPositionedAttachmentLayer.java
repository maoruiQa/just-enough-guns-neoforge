package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
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

public final class GunPositionedAttachmentLayer extends GeoRenderLayer<AnimatedGunItem> {
    private static final String ATTACHMENT_BONE = "attachment_bone";
    private static final String MODEL_ROOT = "geo/item/attachment/";
    private static final String TEXTURE_ROOT = "textures/animated/attachment/";
    private static final String PAINT_JOB_TEXTURE_ROOT = "textures/animated/attachment/paintjob/";
    private static final EnumSet<AttachmentType> RENDERED_TYPES = EnumSet.of(
            AttachmentType.BARREL,
            AttachmentType.UNDER_BARREL,
            AttachmentType.SPECIAL
    );

    private final GunAttachmentGeoModel attachmentModel = new GunAttachmentGeoModel();

    public GunPositionedAttachmentLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
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

        ResourceLocation gunId = gun.getStats().id();
        for (AttachmentType type : RENDERED_TYPES) {
            renderAttachment(type, gunId, gunStack, poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }

    private void renderAttachment(
            AttachmentType type,
            ResourceLocation gunId,
            ItemStack gunStack,
            PoseStack poseStack,
            AnimatedGunItem animatable,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack attachmentStack = GunAttachments.stack(gunStack, type).orElse(ItemStack.EMPTY);
        if (!GunAttachments.canAttachStack(gunStack, type, attachmentStack)) {
            return;
        }

        ResourceLocation attachmentId = GunAttachments.id(gunStack, type).orElse(null);
        if (attachmentId == null) {
            return;
        }

        GunAttachmentTransforms.Transform transform = GunAttachmentTransforms.transform(gunId, type).orElse(null);
        if (transform == null || !transform.isVisible()) {
            return;
        }

        ResourceLocation model = Reference.id(MODEL_ROOT + attachmentId.getPath() + ".geo.json");
        if (!exists(model)) {
            return;
        }

        ResourceLocation texture = texture(attachmentId, gunStack);
        if (!exists(texture)) {
            return;
        }
        BakedGeoModel bakedModel = this.attachmentModel.getBakedModel(model);
        RenderType attachmentRenderType = RenderType.entityTranslucent(texture);
        VertexConsumer attachmentBuffer = bufferSource.getBuffer(attachmentRenderType);

        poseStack.pushPose();
        transform.apply(poseStack);
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
