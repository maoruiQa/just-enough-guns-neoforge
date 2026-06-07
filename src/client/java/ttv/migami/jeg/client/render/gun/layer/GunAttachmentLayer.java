package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunAttachmentGeoModel;
import ttv.migami.jeg.client.render.gun.GunAttachmentTransforms;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;

public final class GunAttachmentLayer extends GeoRenderLayer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> {
    private static final String ATTACHMENT_BONE = "attachment_bone";
    private static final String MODEL_ROOT = "item/attachment/";
    private static final String MODEL_RESOURCE_ROOT = "geckolib/models/item/attachment/";
    private static final String TEXTURE_ROOT = "textures/animated/attachment/";
    private static final String PAINT_JOB_TEXTURE_ROOT = "textures/animated/attachment/paintjob/";
    private static final double SCOPE_MODEL_Y_OFFSET = -3.0D / 16.0D;
    private static final Identifier MAKESHIFT_STOCK = Reference.id("makeshift_stock");
    private static final Set<Identifier> BAKED_UNDER_BARREL_GUNS = Set.of(
            Reference.id("combat_rifle"),
            Reference.id("holy_shotgun"),
            Reference.id("pump_shotgun")
    );
    private static final Set<Identifier> BAKED_STANDARD_STOCK_GUNS = Set.of(
            Reference.id("blossom_rifle"),
            Reference.id("burst_rifle"),
            Reference.id("combat_rifle"),
            Reference.id("hollenfire_mk2"),
            Reference.id("service_rifle")
    );
    private static final Set<Identifier> BAKED_MAKESHIFT_STOCK_GUNS = Set.of(
            Reference.id("abstract_gun"),
            Reference.id("assault_rifle"),
            Reference.id("custom_smg"),
            Reference.id("double_barrel_shotgun"),
            Reference.id("holy_shotgun"),
            Reference.id("phantom_smg"),
            Reference.id("pump_shotgun"),
            Reference.id("revolver"),
            Reference.id("semi_auto_rifle")
    );
    private static final EnumSet<AttachmentType> POSITIONED_TYPES = EnumSet.of(
            AttachmentType.SCOPE,
            AttachmentType.BARREL,
            AttachmentType.STOCK,
            AttachmentType.UNDER_BARREL,
            AttachmentType.SPECIAL
    );
    private static final Map<Identifier, String> VANILLA_SWORD_ATTACHMENTS = Map.of(
            Identifier.fromNamespaceAndPath("minecraft", "wooden_sword"), "wooden_sword",
            Identifier.fromNamespaceAndPath("minecraft", "stone_sword"), "stone_sword",
            Identifier.fromNamespaceAndPath("minecraft", "iron_sword"), "iron_sword",
            Identifier.fromNamespaceAndPath("minecraft", "golden_sword"), "golden_sword",
            Identifier.fromNamespaceAndPath("minecraft", "diamond_sword"), "diamond_sword",
            Identifier.fromNamespaceAndPath("minecraft", "netherite_sword"), "netherite_sword"
    );

    private final GunAttachmentGeoModel attachmentModel = new GunAttachmentGeoModel();

    public GunAttachmentLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
        Item item = passInfo.renderState().getOrDefaultGeckolibData(AnimatedGunRenderer.ANIMATED_ITEM, (Item) null);
        ItemStack gunStack = passInfo.renderState().getOrDefaultGeckolibData(AnimatedGunRenderer.ITEM_STACK, ItemStack.EMPTY);
        if (!(item instanceof AnimatedGunItem gun) || gunStack.isEmpty()) {
            return;
        }

        passInfo.model().getBone(ATTACHMENT_BONE)
                .ifPresent(bone -> passInfo.addPerBoneRender(bone, new AttachmentRenderTask(this.attachmentModel, gun.getStats().id(), gunStack.copy())));
    }

    private static final class AttachmentRenderTask implements PerBoneRender<GeoRenderState> {
        private final GunAttachmentGeoModel attachmentModel;
        private final Identifier gunId;
        private final ItemStack gunStack;

        private AttachmentRenderTask(GunAttachmentGeoModel attachmentModel, Identifier gunId, ItemStack gunStack) {
            this.attachmentModel = attachmentModel;
            this.gunId = gunId;
            this.gunStack = gunStack;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<GeoRenderState> passInfo, GeoBone bone, SubmitNodeCollector collector) {
            renderFirstPersonMuzzleFlash(passInfo, bone, collector);
            bone.translateAwayFromPivotPoint(passInfo.poseStack());
            for (AttachmentType type : POSITIONED_TYPES) {
                renderAttachment(type, passInfo, collector);
            }
            renderBayonet(passInfo, collector);
        }

        private void renderFirstPersonMuzzleFlash(RenderPassInfo<GeoRenderState> passInfo, GeoBone bone, SubmitNodeCollector collector) {
            ItemDisplayContext ctx = passInfo.renderState()
                    .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
            if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                return;
            }

            HumanoidArm arm = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            FabricClientBootstrap.submitFirstPersonMuzzleFlashRelativeToBone(
                    passInfo.poseStack(),
                    collector,
                    this.gunStack,
                    arm,
                    bone.pivotX(),
                    bone.pivotY(),
                    bone.pivotZ()
            );
        }

        private void renderAttachment(AttachmentType type, RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
            ItemStack attachmentStack = GunAttachments.stack(this.gunStack, type).orElse(ItemStack.EMPTY);
            if (!GunAttachments.canAttachStack(this.gunStack, type, attachmentStack)) {
                return;
            }
            if (type == AttachmentType.BARREL && isBayonetStack(attachmentStack)) {
                return;
            }

            Identifier attachmentId = GunAttachments.id(this.gunStack, type).orElse(null);
            if (attachmentId == null || isOwnedByBakedGunModel(type, this.gunId, attachmentId)) {
                return;
            }

            GunAttachmentTransforms.Transform transform = GunAttachmentTransforms.transform(this.gunId, type).orElse(null);
            if (type != AttachmentType.SCOPE && (transform == null || !transform.isVisible())) {
                return;
            }

            String attachmentName = attachmentId.getPath();
            Identifier model = model(attachmentName);
            Identifier texture = texture(attachmentId, this.gunStack);
            boolean modelExists = exists(modelResource(model));
            boolean textureExists = exists(texture);
            if (!modelExists || !textureExists) {
                return;
            }

            BakedGeoModel bakedModel = this.attachmentModel.getBakedModel(model);
            collector.submitCustomGeometry(
                    passInfo.poseStack(),
                    RenderTypes.entityTranslucent(texture),
                    (pose, buffer) -> {
                        List<BoneVisibility> hiddenGlowBones = hideDisabledSpecialGlow(bakedModel, attachmentId, this.gunStack);
                        try {
                            renderModel(bakedModel, passInfo, pose, buffer, attachmentPose -> {
                                if (transform != null) {
                                    transform.apply(attachmentPose);
                                } else {
                                    attachmentPose.translate(0.0D, SCOPE_MODEL_Y_OFFSET, 0.0D);
                                }
                            });
                        } finally {
                            restore(hiddenGlowBones);
                        }
                    }
            );
        }

        private void renderBayonet(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
            ItemStack attachmentStack = GunAttachments.stack(this.gunStack, AttachmentType.BARREL).orElse(ItemStack.EMPTY);
            if (!GunAttachments.canAttachStack(this.gunStack, AttachmentType.BARREL, attachmentStack)) {
                return;
            }

            String attachmentName = bayonetName(attachmentStack);
            if (attachmentName == null) {
                return;
            }

            Identifier model = model(attachmentName);
            Identifier texture = bayonetTexture(attachmentStack, attachmentName, this.gunStack);
            boolean modelExists = exists(modelResource(model));
            boolean textureExists = exists(texture);
            if (!modelExists || !textureExists) {
                return;
            }

            BakedGeoModel bakedModel = this.attachmentModel.getBakedModel(model);
            collector.submitCustomGeometry(
                    passInfo.poseStack(),
                    RenderTypes.entityTranslucent(texture),
                    (pose, buffer) -> {
                        renderModel(bakedModel, passInfo, pose, buffer, attachmentPose ->
                                GunAttachmentTransforms.transform(this.gunId, AttachmentType.BARREL)
                                        .ifPresent(transform -> transform.apply(attachmentPose)));
                    }
            );
        }

        private static void renderModel(
                BakedGeoModel bakedModel,
                RenderPassInfo<GeoRenderState> passInfo,
                PoseStack.Pose pose,
                VertexConsumer buffer,
                Consumer<PoseStack> transform
        ) {
            PoseStack renderPose = passInfo.poseStack();
            renderPose.pushPose();
            try {
                renderPose.last().set(pose);
                transform.accept(renderPose);
                bakedModel.render(passInfo, buffer, passInfo.packedLight(), passInfo.packedOverlay(), passInfo.renderColor());
            } finally {
                renderPose.popPose();
            }
        }
    }

    private static List<BoneVisibility> hideDisabledSpecialGlow(BakedGeoModel model, Identifier attachmentId, ItemStack gunStack) {
        if (Reference.id("flashlight").equals(attachmentId) && !GunAttachments.isFlashlightPowered(gunStack)) {
            return hideBones(model, "glow", "flashlight_glow");
        }
        if (Reference.id("laser_pointer").equals(attachmentId) && !GunAttachments.isLaserPointerPowered(gunStack)) {
            return hideBones(model, "glow");
        }
        return List.of();
    }

    private static List<BoneVisibility> hideBones(BakedGeoModel model, String... boneNames) {
        List<BoneVisibility> hiddenBones = new ArrayList<>();
        for (String boneName : boneNames) {
            model.getBone(boneName).ifPresent(bone -> {
                BoneSnapshot snapshot = bone.frameSnapshot;
                if (snapshot == null) {
                    snapshot = BoneSnapshot.create(bone);
                    bone.frameSnapshot = snapshot;
                }
                hiddenBones.add(new BoneVisibility(snapshot, snapshot.isHidden(), snapshot.areChildrenHidden()));
                snapshot.skipRender(true);
            });
        }
        return hiddenBones;
    }

    private static void restore(List<BoneVisibility> hiddenBones) {
        for (BoneVisibility hiddenBone : hiddenBones) {
            hiddenBone.snapshot().skipRender(hiddenBone.hidden());
            hiddenBone.snapshot().skipChildrenRender(hiddenBone.childrenHidden());
        }
    }
    private static Identifier model(String attachmentName) {
        return Reference.id(MODEL_ROOT + attachmentName);
    }

    private static Identifier modelResource(Identifier model) {
        return Reference.id(MODEL_RESOURCE_ROOT + model.getPath().substring(MODEL_ROOT.length()) + ".geo.json");
    }

    private static boolean isOwnedByBakedGunModel(AttachmentType type, Identifier gunId, Identifier attachmentId) {
        if (type == AttachmentType.UNDER_BARREL) {
            return BAKED_UNDER_BARREL_GUNS.contains(gunId);
        }
        if (type != AttachmentType.STOCK) {
            return false;
        }
        if (MAKESHIFT_STOCK.equals(attachmentId)) {
            return BAKED_MAKESHIFT_STOCK_GUNS.contains(gunId);
        }
        return BAKED_STANDARD_STOCK_GUNS.contains(gunId);
    }

    private static String bayonetName(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String vanilla = VANILLA_SWORD_ATTACHMENTS.get(id);
        if (vanilla != null) {
            return vanilla;
        }
        return isBayonetStack(stack) ? "modded_sword" : null;
    }

    private static boolean isBayonetStack(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().endsWith("_sword");
    }

    private static Identifier texture(Identifier attachmentId, ItemStack gunStack) {
        String attachment = attachmentId.getPath();
        String paintJob = paintJob(gunStack);
        if (paintJob != null) {
            Identifier painted = Reference.id(PAINT_JOB_TEXTURE_ROOT + paintJob + "/" + attachment + ".png");
            if (exists(painted)) {
                return painted;
            }
        }

        Identifier base = Reference.id(TEXTURE_ROOT + attachment + ".png");
        if (exists(base)) {
            return base;
        }
        return Reference.id(TEXTURE_ROOT + "reflex_sight.png");
    }

    private static Identifier bayonetTexture(ItemStack attachmentStack, String attachmentName, ItemStack gunStack) {
        Identifier attachmentId = BuiltInRegistries.ITEM.getKey(attachmentStack.getItem());
        if ("modded_sword".equals(attachmentName) && attachmentId != null) {
            return Identifier.fromNamespaceAndPath(attachmentId.getNamespace(), "textures/item/" + attachmentId.getPath() + ".png");
        }

        String paintJob = paintJob(gunStack);
        if (paintJob != null) {
            Identifier painted = Reference.id(PAINT_JOB_TEXTURE_ROOT + paintJob + "/" + attachmentName + ".png");
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

    private static boolean exists(Identifier id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }

    private record BoneVisibility(BoneSnapshot snapshot, boolean hidden, boolean childrenHidden) {
    }
}
