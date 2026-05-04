package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;

public record GunPoseProfile(
        ArmMode armMode,
        boolean renderLeftArm,
        float hipX,
        float hipY,
        float hipZ,
        float adsX,
        float adsY,
        float adsZ,
        float hipYaw,
        float adsYaw,
        float scale,
        ArmTransform leftArm,
        ArmTransform rightArm,
        boolean canApplySprintingAnimation
) {
    public record ArmTransform(
            float tx,
            float ty,
            float tz,
            float rx,
            float ry,
            float rz,
            float sx,
            float sy,
            float sz
    ) {}

    public enum ArmMode {
        ONE_HANDED,
        TWO_HANDED,
        HEAVY
    }

    private static final ArmTransform DEFAULT_LEFT_ARM = arm(-0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F);
    private static final ArmTransform DEFAULT_RIGHT_ARM = arm(0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F);

    private static final GunPoseProfile DEFAULT = of(
            ArmMode.TWO_HANDED,
            true,
            0.48F, 0.16F, -0.58F,
            0.24F, 0.04F, -0.80F,
            3.0F, 4.0F, 1.25F
    );

    private static final GunPoseProfile CUSTOM_SMG_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.52F, -0.78F,
            3.0F, 3.4F, 1.22F,
            // 1.20.1 custom_smg idle anim drives arm orientation/offset via arm bones;
            // keep profile correction near-neutral so those bone transforms remain dominant.
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile HOLY_SHOTGUN_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.895F, -1.04F, -1.45F,
            0.29F, -0.64F, -1.32F,
            3.0F, 1.8F, 1.22F,
            // 1.20.1 custom_smg idle anim drives arm orientation/offset via arm bones;
            // keep profile correction near-neutral so those bone transforms remain dominant.
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile COMBAT_RIFLE_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.58F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile HOLLENFIRE_MK2_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.58F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile SUPERSONIC_SHOTGUN_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.56F, -0.86F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile ROCKET_LAUNCHER_PROFILE = of(
            ArmMode.HEAVY,
            false,
            0.64F, -0.46F, -0.72F,
            0.35F, -0.64F, -0.88F,
            2.0F, 4.0F, 1.22F,
            arm(-0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
            arm(0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
    );

    private static final GunPoseProfile TYPHOONEE_PROFILE = of(
            ArmMode.HEAVY,
            false,
            0.64F, -1.56F, -0.30F,
            0.35F, -1.72F, -0.46F,
            2.0F, 4.0F, 1.22F,
            arm(-0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
            arm(0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
    );

    private static final Map<ResourceLocation, GunPoseProfile> PROFILES = Map.ofEntries(
            Map.entry(Reference.id("finger_gun"), of(
                    ArmMode.ONE_HANDED,
                    false,
                    0.76F, -0.48F, -0.34F,
                    0.70F, -0.45F, -0.38F,
                    0.0F, 0.0F, 1.25F
            )),
            Map.entry(Reference.id("combat_pistol"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("semi_auto_pistol"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("revolver"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("flare_gun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("custom_smg"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("assault_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("burst_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("semi_auto_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("bolt_action_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("combat_rifle"), COMBAT_RIFLE_PROFILE),
            Map.entry(Reference.id("infantry_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("service_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("blossom_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("subsonic_rifle"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("hollenfire_mk2"), HOLLENFIRE_MK2_PROFILE),
            Map.entry(Reference.id("light_machine_gun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("double_barrel_shotgun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("waterpipe_shotgun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("pump_shotgun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("repeating_shotgun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("supersonic_shotgun"), SUPERSONIC_SHOTGUN_PROFILE),
            Map.entry(Reference.id("holy_shotgun"), HOLY_SHOTGUN_PROFILE),
            Map.entry(Reference.id("shotgun"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("grenade_launcher"), CUSTOM_SMG_PROFILE),
            Map.entry(Reference.id("typhoonee"), TYPHOONEE_PROFILE),
            Map.entry(Reference.id("rocket_launcher"), of(
                    ArmMode.HEAVY,
                    true,
                    0.32F, -0.35F, -0.85F,
                    0.32F, -0.40F, -0.90F,
                    1.8F, 3.0F, 1.10F,
                    arm(-0.08F, -0.10F, 0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.20F, -0.10F, 0.00F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
            )),
            Map.entry(Reference.id("minigun"), of(
                    ArmMode.HEAVY,
                    true,
                    0.32F, -0.46F, -0.30F,
                    0.24F, -0.64F, -0.46F,
                    2.0F, 4.0F, 1.22F,
                    arm(-0.08F, -0.10F, 0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.20F, -0.10F, 0.00F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
            )),
            Map.entry(Reference.id("hypersonic_cannon"), of(
                    ArmMode.HEAVY,
                    true,
                    0.64F, -0.44F, -0.74F,
                    0.24F, -0.60F, -0.88F,
                    2.0F, 4.0F, 1.22F,
                    arm(-0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
            )),
            Map.entry(Reference.id("flamethrower"), of(
                    ArmMode.HEAVY,
                    true,
                    0.56F, -0.42F, -0.70F,
                    0.28F, -0.52F, -0.84F,
                    2.0F, 3.0F, 1.22F,
                    arm(0.42F, -0.40F, 0.14F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.82F, -0.40F, 0.14F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
            ))
    );

    private static GunPoseProfile of(
            ArmMode armMode,
            boolean renderLeftArm,
            float hipX,
            float hipY,
            float hipZ,
            float adsX,
            float adsY,
            float adsZ,
            float hipYaw,
            float adsYaw,
            float scale
    ) {
        return new GunPoseProfile(
                armMode,
                renderLeftArm,
                hipX,
                hipY,
                hipZ,
                adsX,
                adsY,
                adsZ,
                hipYaw,
                adsYaw,
                scale,
                DEFAULT_LEFT_ARM,
                DEFAULT_RIGHT_ARM,
                true
        );
    }

    private static GunPoseProfile of(
            ArmMode armMode,
            boolean renderLeftArm,
            float hipX,
            float hipY,
            float hipZ,
            float adsX,
            float adsY,
            float adsZ,
            float hipYaw,
            float adsYaw,
            float scale,
            ArmTransform leftArm,
            ArmTransform rightArm
    ) {
        return new GunPoseProfile(
                armMode,
                renderLeftArm,
                hipX,
                hipY,
                hipZ,
                adsX,
                adsY,
                adsZ,
                hipYaw,
                adsYaw,
                scale,
                leftArm,
                rightArm,
                true
        );
    }

    private static ArmTransform arm(
            float tx,
            float ty,
            float tz,
            float rx,
            float ry,
            float rz,
            float sx,
            float sy,
            float sz
    ) {
        return new ArmTransform(tx, ty, tz, rx, ry, rz, sx, sy, sz);
    }

    public static GunPoseProfile forGun(ResourceLocation gunId) {
        return PROFILES.getOrDefault(gunId, DEFAULT);
    }
}
