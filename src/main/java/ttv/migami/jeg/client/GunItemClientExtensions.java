package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForgeVersion;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.GunAttachmentTransforms;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;

public final class GunItemClientExtensions implements IClientItemExtensions {
    private static final boolean NEOFORGE_APPLIES_GENERIC_HAND_TRANSFORM =
            neoforgeVersionAtLeast(26, 1, 2);
    private static final float FIRST_PERSON_GLOBAL_SCALE_MULTIPLIER = 1.28F;
    private static final float SCOPED_BOLT_ACTION_SCALE_MULTIPLIER = 1.1F;
    private static final double FIRST_PERSON_TRANSLATE_X = -3.0D / 16.0D;
    private static final double FIRST_PERSON_TRANSLATE_Y = 0.0D;
    private static final double FIRST_PERSON_TRANSLATE_Z = -3.0D / 16.0D;
    private static final double FORGE_MODEL_CENTER_OFFSET = 0.5D;
    private static final double FORGE_UNIT = 1.0D / 16.0D;
    private static final double FORGE_GUN_ORIGIN_X = 8.0D;
    private static final double FORGE_GUN_ORIGIN_Y = 0.0D;
    private static final double FORGE_GUN_ORIGIN_Z = 8.0D;
    private static final double OPEN_SIGHT_SCOPE_CAMERA_X = 0.0D;
    private static final double REFLEX_SCOPE_CAMERA_Y = 1.05D;
    private static final double MONOCLE_SCOPE_CAMERA_Y = 0.85D;
    private static final double OPEN_SIGHT_SCOPE_CAMERA_Z = 12.0D;
    private static final double FORGE_SCOPE_Y_OFFSET = 0.54D;
    private static final double FORGE_SCOPE_Z_OFFSET = -0.16D;
    private static final double FORGE_IRON_SIGHT_Y_OFFSET = 0.6059D;
    private static final double FORGE_IRON_SIGHT_Z_OFFSET = -0.16D;
    private static final double FORGE_LEGACY_IRON_SIGHT_Z_OFFSET = 0.72D;
    private static final Identifier REFLEX_SIGHT = Reference.id("reflex_sight");
    private static final Identifier MONOCLE_SIGHT = Reference.id("monocle_sight");
    private static final Identifier HOLOGRAPHIC_SIGHT = Reference.id("holographic_sight");
    private static final Identifier TELESCOPIC_SIGHT = Reference.id("telescopic_sight");
    private static final Identifier SPYGLASS = Identifier.withDefaultNamespace("spyglass");
    private static final Map<Identifier, Map<Identifier, ForgeZoomOffset>> RIFLE_SCOPE_ADS_CORRECTIONS = Map.of(
            Reference.id("combat_rifle"), Map.of(
                    TELESCOPIC_SIGHT, zoom(0.0D, 1.13D, 0.0D),
                    SPYGLASS, zoom(0.0D, -0.68D, 0.0D)
            ),
            Reference.id("service_rifle"), Map.of(
                    TELESCOPIC_SIGHT, zoom(0.0D, 0.55D, 0.0D),
                    SPYGLASS, zoom(0.0D, -0.75D, 0.0D)
            )
    );
    private static final Map<Identifier, ForgeZoomOffset> FORGE_ZOOM_OFFSETS = Map.ofEntries(
            Map.entry(Reference.id("abstract_gun"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("assault_rifle"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("blossom_rifle"), zoom(0.0D, 4.75D, -1.5D)),
            Map.entry(Reference.id("bolt_action_rifle"), zoom(0.0D, 4.35D, -1.25D)),
            Map.entry(Reference.id("burst_rifle"), zoom(0.0D, 4.65D, -1.75D)),
            Map.entry(Reference.id("combat_pistol"), zoom(0.0D, 5.135D, -1.75D)),
            Map.entry(Reference.id("combat_rifle"), zoom(0.0D, 5.71D, -1.75D)),
            Map.entry(Reference.id("custom_smg"), zoom(0.0D, 3.85D, -1.75D)),
            Map.entry(Reference.id("double_barrel_shotgun"), zoom(0.0D, 5.05D, 0.75D)),
            Map.entry(Reference.id("finger_gun"), zoom(0.0D, 3.0D, -1.75D)),
            Map.entry(Reference.id("flamethrower"), zoom(0.0D, 5.71D, -1.75D)),
            Map.entry(Reference.id("flare_gun"), zoom(0.0D, 4.85D, -1.75D)),
            Map.entry(Reference.id("grenade_launcher"), zoom(0.0D, 3.61D, 2.0D)),
            Map.entry(Reference.id("hollenfire_mk2"), zoom(0.0D, 5.2D, -1.75D)),
            Map.entry(Reference.id("holy_shotgun"), zoom(0.0D, 3.14D, -1.25D)),
            Map.entry(Reference.id("hypersonic_cannon"), zoom(0.0D, 3.9D, -2.25D)),
            Map.entry(Reference.id("infantry_rifle"), zoom(0.0D, 3.8D, 1.75D)),
            Map.entry(Reference.id("light_machine_gun"), zoom(0.0D, 4.56D, -1.75D)),
            Map.entry(Reference.id("pump_shotgun"), zoom(0.0D, 3.3D, -1.25D)),
            Map.entry(Reference.id("repeating_shotgun"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("revolver"), zoom(0.0D, 3.85D, -1.75D)),
            Map.entry(Reference.id("rocket_launcher"), zoom(2.8D, 3.4D, -1.75D)),
            Map.entry(Reference.id("semi_auto_pistol"), zoom(0.0D, 4.965D, -1.75D)),
            Map.entry(Reference.id("semi_auto_rifle"), zoom(0.0D, 3.6D, -1.25D)),
            Map.entry(Reference.id("service_rifle"), zoom(0.0D, 5.2D, -1.75D)),
            Map.entry(Reference.id("soulhunter_mk2"), zoom(0.0D, 4.415D, 2.0D)),
            Map.entry(Reference.id("subsonic_rifle"), zoom(0.0D, 3.95D, -0.75D)),
            Map.entry(Reference.id("supersonic_shotgun"), zoom(0.0D, 4.2D, -3.75D)),
            Map.entry(Reference.id("typhoonee"), zoom(0.0D, 4.45D, -1.25D)),
            Map.entry(Reference.id("waterpipe_shotgun"), zoom(0.0D, 3.65D, 0.75D))
    );
    private final GunStats stats;

    public GunItemClientExtensions(GunItem item) {
        this.stats = item.getStats();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        // Match NeoForge 1.21.10: third-person two-handed hold pose for guns.
        // First-person pose is handled by applyForgeHandTransform + GeckoLib arms layer for AnimatedGunItem.
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack itemInHand,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        if (NEOFORGE_APPLIES_GENERIC_HAND_TRANSFORM) {
            return false;
        }
        return applyForStats(this.stats, poseStack, player, arm, partialTick, equipProcess, swingProcess);
    }

    public static boolean applyForStats(
            GunStats stats,
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            float partialTick,
            float equipProcess,
            float swingProcess
    ) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        String gunPath = stats.id().getPath();
        boolean isBow = gunPath.contains("bow");
        float ads = AimingHandler.get().getRenderAdsProgress();
        GunPoseProfile profile = GunPoseProfile.forGun(stats.id());

        float xOffset = Mth.lerp(ads, profile.hipX(), profile.adsX());
        float yOffset = Mth.lerp(ads, profile.hipY(), profile.adsY());
        float zOffset = Mth.lerp(ads, profile.hipZ(), profile.adsZ());
        float yaw = Mth.lerp(ads, profile.hipYaw(), profile.adsYaw());
        float adsTransition = (float) easeOutQuad(ads);
        float firstPersonScale = profile.scale() * FIRST_PERSON_GLOBAL_SCALE_MULTIPLIER;
        ItemStack stack = heldStackForArm(player, arm);
        boolean telescopicSight = GunScopeSupport.hasTelescopicSight(stack);
        if (telescopicSight) {
            firstPersonScale *= SCOPED_BOLT_ACTION_SCALE_MULTIPLIER;
        }
        AdsSightOffset sightOffset = adsSightOffset(stack, stats.id(), firstPersonScale);
        xOffset = Mth.lerp(adsTransition, xOffset, (float) sightOffset.x());
        yOffset = Mth.lerp(adsTransition, yOffset, (float) sightOffset.y());
        zOffset = Mth.lerp(adsTransition, zOffset, (float) sightOffset.z());
        yaw = Mth.lerp(adsTransition, yaw, 0.0F);

        poseStack.translate(direction * xOffset, yOffset, zOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 0.5F * (1.0F - ads)));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(adsTransition, 4.0F, 0.8F)));
        poseStack.scale(firstPersonScale, firstPersonScale, firstPersonScale);

        // Keep vanilla-like equip/swing movement to reduce "hard snap" while switching items.
        float equip = Mth.clamp(equipProcess, 0.0F, 1.0F);
        poseStack.translate(0.0F, -0.6F * equip, 0.0F);
        if (!isBow) {
            float swingRoot = Mth.sqrt(swingProcess);
            float swingX = -0.3F * Mth.sin(swingRoot * (float) Math.PI);
            float swingY = 0.4F * Mth.sin(swingRoot * ((float) Math.PI * 2F));
            poseStack.translate(direction * swingX * 0.08F, swingY * 0.05F, 0.0F);
        }

        applyBobbingTransforms(poseStack, player, partialTick, ads);
        applySwayTransforms(poseStack, player, partialTick, ads);
        applySprintingTransforms(poseStack, player, stack, direction, ads);
        applyRecoilTransforms(poseStack, ads);
        return true;
    }

    private static ItemStack heldStackForArm(LocalPlayer player, HumanoidArm arm) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        boolean mainHandArm = player.getMainArm() == arm;
        return mainHandArm ? player.getMainHandItem() : player.getOffhandItem();
    }

    private static void applyBobbingTransforms(PoseStack poseStack, Player player, float partialTick, float ads) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.bobView().get() || minecraft.getCameraEntity() != player) {
            return;
        }

        if (player.onGround() && !player.isPassenger()) {
            float speed = (float) player.getDeltaMovement().horizontalDistance();
            float time = (player.tickCount + partialTick) * 0.5F;
            float bobAmount = Mth.sin(time) * speed * 0.15F;
            float adsDamping = 1.0F - ads * 0.7F;
            poseStack.translate(0.0D, bobAmount * adsDamping, 0.0D);
        }
    }

    private static void applySwayTransforms(PoseStack poseStack, LocalPlayer player, float partialTick, float ads) {
        if (player.onGround() && !player.isPassenger()) {
            float speed = (float) player.getDeltaMovement().horizontalDistance();
            float time = (player.tickCount + partialTick) * 0.3F;
            float swayX = Mth.sin(time) * speed * 2.0F;
            float swayZ = Mth.cos(time * 0.7F) * speed * 2.0F;
            float adsDamping = 1.0F - ads * 0.8F;
            poseStack.mulPose(Axis.XP.rotationDegrees(swayX * adsDamping));
            poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ * adsDamping));
        }

        float fallDelta = (float) Mth.clamp(player.yo - player.getY(), -1.0D, 1.0D);
        fallDelta *= (1.0F - ads * 0.5F) * (1.0F - Mth.abs(player.getXRot()) / 90.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(fallDelta * 12.0F));
    }

    private static void applySprintingTransforms(PoseStack poseStack, LocalPlayer player, ItemStack stack, int direction, float ads) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean attackDown = minecraft != null && minecraft.player == player && minecraft.options.keyAttack.isDown();
        if (!player.isSprinting() || AimingHandler.get().isAiming() || attackDown
                || GunRecoilHandler.isSuppressingSprintPose() || hasBayonet(stack)) {
            return;
        }

        float transition = 1.0F - ads;
        poseStack.translate(-0.18F * direction * transition, -0.08F * transition, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(30.0F * direction * transition));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * transition));
    }

    private static boolean hasBayonet(ItemStack stack) {
        return GunAttachments.stack(stack, AttachmentType.BARREL)
                .filter(GunItemClientExtensions::isBayonetStack)
                .isPresent();
    }

    private static boolean isBayonetStack(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().endsWith("_sword");
    }

    private static void applyRecoilTransforms(PoseStack poseStack, float ads) {
        double recoilNormal = GunRecoilHandler.getGunRecoilNormal();
        if (recoilNormal <= 0.0D) {
            return;
        }

        float adsReduction = GunRecoilHandler.getAdsRecoilReduction();
        double kick = GunRecoilHandler.getGunRecoilKick() * 0.0625D * recoilNormal * adsReduction;
        float recoilLift = (float) (GunRecoilHandler.getGunRecoilAngle() * recoilNormal) * adsReduction;
        float recoilSwayAmount = 2.0F + 1.0F * (1.0F - ads);
        float recoilSway = (float) ((GunRecoilHandler.getGunRecoilRandom() * recoilSwayAmount - recoilSwayAmount / 2.0F) * recoilNormal);
        poseStack.translate(0.0D, 0.0D, kick);
        poseStack.translate(0.0D, 0.0D, 0.15D);
        poseStack.mulPose(Axis.YP.rotationDegrees(recoilSway));
        poseStack.mulPose(Axis.ZP.rotationDegrees(recoilSway));
        poseStack.mulPose(Axis.XP.rotationDegrees(recoilLift));
        poseStack.translate(0.0D, 0.0D, -0.15D);
    }

    private static double easeOutQuad(double value) {
        double inverse = 1.0D - Mth.clamp(value, 0.0D, 1.0D);
        return 1.0D - inverse * inverse;
    }

    private static AdsSightOffset adsSightOffset(ItemStack stack, Identifier gunId, double firstPersonScale) {
        double translateX = FIRST_PERSON_TRANSLATE_X;
        double translateY = FIRST_PERSON_TRANSLATE_Y;
        double translateZ = FIRST_PERSON_TRANSLATE_Z;
        double scaleX = 1.0D;
        double scaleY = 1.0D;
        double scaleZ = 1.0D;

        Double openSightCameraY = openSightScopeCameraY(stack);
        ForgeZoomOffset adsOffset = openSightCameraY != null
                ? openSightScopeAdsOffset(gunId, openSightCameraY, translateX, translateY, translateZ, scaleX, scaleY, scaleZ)
                : null;
        if (adsOffset == null) {
            boolean telescopicSight = GunScopeSupport.hasTelescopicSight(stack);
            ForgeZoomOffset zoom = telescopicSight
                    ? zoom(0.0D, 5.0D, -4.4D)
                    : FORGE_ZOOM_OFFSETS.getOrDefault(gunId, FORGE_ZOOM_OFFSETS.get(Reference.id("abstract_gun")));
            if (!telescopicSight) {
                zoom = withAttachmentAdsOffset(zoom, GunAttachments.modifiers(stack));
            }
            zoom = withRifleScopeAdsCorrection(stack, gunId, zoom);
            adsOffset = ironSightAdsOffset(zoom, translateX, translateY, translateZ, scaleX, scaleY, scaleZ);
        }

        return new AdsSightOffset(
                -0.56D - adsOffset.xOffset() * firstPersonScale,
                0.52D - adsOffset.yOffset() * firstPersonScale,
                0.72D - adsOffset.zOffset() * firstPersonScale
        );
    }

    private static ForgeZoomOffset ironSightAdsOffset(ForgeZoomOffset zoom, double translateX, double translateY, double translateZ, double scaleX, double scaleY, double scaleZ) {
        double xOffset = translateX - FORGE_MODEL_CENTER_OFFSET * scaleX
                + FORGE_MODEL_CENTER_OFFSET * scaleX
                - zoom.xOffset() * FORGE_UNIT * scaleX;
        double yOffset = translateY - FORGE_MODEL_CENTER_OFFSET * scaleY
                + zoom.yOffset() * FORGE_UNIT * scaleY
                + FORGE_IRON_SIGHT_Y_OFFSET;
        double zOffset = translateZ - FORGE_MODEL_CENTER_OFFSET * scaleZ
                + FORGE_MODEL_CENTER_OFFSET * scaleZ
                - zoom.zOffset() * FORGE_UNIT * scaleZ
                + FORGE_IRON_SIGHT_Z_OFFSET + FORGE_LEGACY_IRON_SIGHT_Z_OFFSET;
        return zoom(xOffset, yOffset, zOffset);
    }

    private static ForgeZoomOffset openSightScopeAdsOffset(Identifier gunId, double scopeCameraY, double translateX, double translateY, double translateZ, double scaleX, double scaleY, double scaleZ) {
        GunAttachmentTransforms.Transform scopeTransform = GunAttachmentTransforms.transform(gunId, AttachmentType.SCOPE).orElse(null);
        if (scopeTransform == null || !scopeTransform.isVisible()) {
            return null;
        }
        double scopeScale = scopeTransform.scale();
        double xOffset = translateX - FORGE_MODEL_CENTER_OFFSET * scaleX
                + FORGE_GUN_ORIGIN_X * FORGE_UNIT * scaleX
                + scopeTransform.x() * FORGE_UNIT * scaleX
                + OPEN_SIGHT_SCOPE_CAMERA_X * FORGE_UNIT * scaleX * scopeScale;
        double yOffset = translateY - FORGE_MODEL_CENTER_OFFSET * scaleY
                + FORGE_GUN_ORIGIN_Y * FORGE_UNIT * scaleY
                + scopeTransform.y() * FORGE_UNIT * scaleY
                + ((scopeCameraY * FORGE_UNIT * scaleY) + FORGE_SCOPE_Y_OFFSET) * scopeScale;
        double zOffset = translateZ - FORGE_MODEL_CENTER_OFFSET * scaleZ
                + FORGE_GUN_ORIGIN_Z * FORGE_UNIT * scaleZ
                + scopeTransform.z() * FORGE_UNIT * scaleZ
                + ((OPEN_SIGHT_SCOPE_CAMERA_Z * FORGE_UNIT * scaleZ) + FORGE_SCOPE_Z_OFFSET) * scopeScale;
        return zoom(xOffset, yOffset, zOffset);
    }

    private record AdsSightOffset(double x, double y, double z) {}

    private record ForgeZoomOffset(double xOffset, double yOffset, double zOffset) {}

    private static ForgeZoomOffset zoom(double xOffset, double yOffset, double zOffset) {
        return new ForgeZoomOffset(xOffset, yOffset, zOffset);
    }

    private static ForgeZoomOffset withAttachmentAdsOffset(ForgeZoomOffset zoom, AttachmentModifiers modifiers) {
        return new ForgeZoomOffset(
                zoom.xOffset() + modifiers.adsViewXOffset(),
                zoom.yOffset() + modifiers.adsViewYOffset(),
                zoom.zOffset() + modifiers.adsViewZOffset()
        );
    }

    private static ForgeZoomOffset withRifleScopeAdsCorrection(ItemStack stack, Identifier gunId, ForgeZoomOffset zoom) {
        Map<Identifier, ForgeZoomOffset> corrections = RIFLE_SCOPE_ADS_CORRECTIONS.get(gunId);
        if (corrections == null) {
            return zoom;
        }
        Identifier scopeId = GunAttachments.id(stack, AttachmentType.SCOPE).orElse(null);
        if (scopeId == null) {
            return zoom;
        }
        ForgeZoomOffset correction = corrections.get(scopeId);
        if (correction != null) {
            return withCorrection(zoom, correction);
        }
        return zoom;
    }

    private static ForgeZoomOffset withCorrection(ForgeZoomOffset zoom, ForgeZoomOffset correction) {
        return new ForgeZoomOffset(
                zoom.xOffset() + correction.xOffset(),
                zoom.yOffset() + correction.yOffset(),
                zoom.zOffset() + correction.zOffset()
        );
    }

    private static Double openSightScopeCameraY(ItemStack stack) {
        Identifier scopeId = GunAttachments.id(stack, AttachmentType.SCOPE).orElse(null);
        if (REFLEX_SIGHT.equals(scopeId)) {
            return REFLEX_SCOPE_CAMERA_Y;
        }
        if (MONOCLE_SIGHT.equals(scopeId) || HOLOGRAPHIC_SIGHT.equals(scopeId)) {
            return MONOCLE_SCOPE_CAMERA_Y;
        }
        return null;
    }

    private static boolean neoforgeVersionAtLeast(int targetMajor, int targetMinor, int targetPatch) {
        int[] version = parseLeadingVersion(NeoForgeVersion.getVersion());
        if (version[0] != targetMajor) {
            return version[0] > targetMajor;
        }
        if (version[1] != targetMinor) {
            return version[1] > targetMinor;
        }
        return version[2] >= targetPatch;
    }

    private static int[] parseLeadingVersion(String version) {
        int[] parsed = new int[] {0, 0, 0};
        if (version == null || version.isEmpty()) {
            return parsed;
        }

        String[] parts = version.split("\\.");
        for (int i = 0; i < parsed.length && i < parts.length; i++) {
            parsed[i] = parseLeadingInt(parts[i]);
        }
        return parsed;
    }

    private static int parseLeadingInt(String value) {
        int result = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            result = result * 10 + c - '0';
        }
        return result;
    }
}
