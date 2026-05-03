package ttv.migami.jeg.client.render.gun;

import java.util.Map;
import net.minecraft.resources.Identifier;
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
            0.24F, 0.08F, -0.80F,
            3.0F, 4.0F, 1.25F
    );

    private static final GunPoseProfile CUSTOM_SMG_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.52F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile LONG_GUN_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.46F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile LOWER_ADS_LONG_GUN_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.52F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile COMBAT_RIFLE_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.54F, -0.60F,
            0.232F, -0.58F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile HOLLENFIRE_MK2_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.44F, -0.60F,
            0.232F, -0.52F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile SERVICE_RIFLE_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.54F, -0.60F,
            0.232F, -0.56F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile SOULHUNTER_MK2_PROFILE = of(
            ArmMode.TWO_HANDED,
            true,
            0.42F, -0.58F, -0.60F,
            0.232F, -0.51F, -0.78F,
            3.0F, 3.4F, 1.22F,
            arm(0.04F, -0.02F, 0.08F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F),
            arm(0.36F, -0.10F, 0.12F, 0.0F, 0.0F, 0.0F, 0.60F, 0.74F, 0.60F)
    );

    private static final GunPoseProfile ROCKET_LAUNCHER_PROFILE = of(
            ArmMode.HEAVY,
            true,
            0.71F, -0.28F, -0.64F,
            0.42F, -0.44F, -0.80F,
            2.0F, 4.0F, 1.22F,
            arm(0.42F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
            arm(0.80F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
    );

    private static final GunPoseProfile TYPHOONEE_PROFILE = of(
            ArmMode.HEAVY,
            false,
            0.64F, -1.56F, -1.28F,
            0.35F, -1.72F, -1.44F,
            2.0F, 4.0F, 1.22F,
            arm(0.42F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
            arm(0.80F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F)
    );

    private static final Map<Identifier, GunPoseProfile> PROFILES = Map.ofEntries(
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
            Map.entry(Reference.id("custom_smg"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("assault_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("burst_rifle"), LOWER_ADS_LONG_GUN_PROFILE),
            Map.entry(Reference.id("semi_auto_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("bolt_action_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("combat_rifle"), COMBAT_RIFLE_PROFILE),
            Map.entry(Reference.id("infantry_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("service_rifle"), SERVICE_RIFLE_PROFILE),
            Map.entry(Reference.id("blossom_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("subsonic_rifle"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("hollenfire_mk2"), HOLLENFIRE_MK2_PROFILE),
            Map.entry(Reference.id("soulhunter_mk2"), SOULHUNTER_MK2_PROFILE),
            Map.entry(Reference.id("light_machine_gun"), LOWER_ADS_LONG_GUN_PROFILE),
            Map.entry(Reference.id("double_barrel_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("waterpipe_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("pump_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("repeating_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("supersonic_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("holy_shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("shotgun"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("grenade_launcher"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("compound_bow"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("primitive_bow"), LONG_GUN_PROFILE),
            Map.entry(Reference.id("typhoonee"), TYPHOONEE_PROFILE),
            Map.entry(Reference.id("rocket_launcher"), ROCKET_LAUNCHER_PROFILE),
            Map.entry(Reference.id("minigun"), of(
                    ArmMode.HEAVY,
                    true,
                    0.32F, -0.31F, -0.97F,
                    0.34F, -0.36F, -1.02F,
                    1.8F, 3.0F, 1.10F,
                    arm(-1.40F, -0.46F, 1.75F, -8.0F, -31.0F, -75.0F, 0.52F, 0.66F, 0.52F),
                    arm(0.18F, -0.10F, -0.04F, -4.0F, 2.0F, -8.0F, 0.54F, 0.68F, 0.54F),
                    false
            )),
            Map.entry(Reference.id("hypersonic_cannon"), of(
                    ArmMode.HEAVY,
                    true,
                    0.64F, -0.44F, -0.74F,
                    0.35F, -0.60F, -0.88F,
                    2.0F, 4.0F, 1.22F,
                    arm(-0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.20F, -0.08F, -0.02F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    true
            )),
            Map.entry(Reference.id("flamethrower"), of(
                    ArmMode.HEAVY,
                    true,
                    0.62F, -0.48F, -0.70F,
                    0.22F, -0.58F, -0.84F,
                    2.0F, 3.0F, 1.22F,
                    arm(0.42F, -0.40F, 0.14F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    arm(0.82F, -0.40F, 0.14F, 0.0F, 0.0F, 0.0F, 0.58F, 0.72F, 0.58F),
                    true
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
            ArmTransform rightArm,
            boolean canApplySprintingAnimation
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
                canApplySprintingAnimation
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

    public static GunPoseProfile forGun(Identifier gunId) {
        return PROFILES.getOrDefault(gunId, DEFAULT);
    }
}
