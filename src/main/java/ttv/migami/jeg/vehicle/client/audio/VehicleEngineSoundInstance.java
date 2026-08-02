package ttv.migami.jeg.vehicle.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/**
 * Client looping engine sound with smooth distance falloff (smoothstep + soft power curve).
 */
public final class VehicleEngineSoundInstance extends AbstractTickableSoundInstance {
    /** Gun fire volume 7.5 × 16 ≈ 120 blocks baseline. */
    public static final float GUN_FIRE_RANGE_BLOCKS = 7.5F * 16.0F;
    /** Near-field loudness (audible but below old server spam of ~3–9). */
    private static final float SOURCE_LOUDNESS = 2.4F;
    private static final int FADE_TICKS = 6;

    private final VehicleEntity vehicle;
    private final SoundEvent sound;
    private int fade;
    private boolean dying;

    public VehicleEngineSoundInstance(VehicleEntity vehicle, SoundEvent sound) {
        super(sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.sound = sound;
        this.looping = true;
        this.delay = 0;
        // Non-zero so the sound engine actually starts the instance (volume 0 is culled)
        this.volume = 0.05F;
        this.pitch = 1.0F;
        // Manual distance curve
        this.attenuation = Attenuation.NONE;
        this.relative = false;
        this.fade = 1;
        this.dying = false;
        this.updatePosition();
    }

    public boolean matches(SoundEvent sound) {
        return this.sound == sound;
    }

    public VehicleEntity vehicle() {
        return this.vehicle;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || this.vehicle.isRemoved() || !this.vehicle.isAlive()) {
            this.stop();
            return;
        }

        if (!this.vehicle.shouldPlayEngineSound()) {
            this.dying = true;
        } else {
            this.dying = false;
        }

        if (this.dying) {
            if (this.fade > 0) {
                this.fade--;
            } else {
                this.stop();
                return;
            }
        } else if (this.fade < FADE_TICKS) {
            this.fade++;
        }

        this.updatePosition();

        double distance = this.vehicle.position().distanceTo(player.position());
        float maxRange = this.maxHearDistance();
        float proximity = 1.0F - Mth.clamp((float) (distance / maxRange), 0.0F, 1.0F);
        // Smoothstep
        float curve = proximity * proximity * (3.0F - 2.0F * proximity);
        // Softer mid-range roll-off (still continuous, no hard cliff)
        curve = (float) Math.pow(curve, 1.15D);

        float config = Math.max(0.2F, this.vehicle.vehicleData().defaults().engine().engineSoundVolume());
        float fadeFactor = this.fade / (float) FADE_TICKS;
        // Rider: slightly louder when sitting in this vehicle
        float riderBoost = player.getVehicle() == this.vehicle ? 1.15F : 1.0F;
        this.volume = Mth.clamp(SOURCE_LOUDNESS * config * curve * fadeFactor * riderBoost, 0.0F, 4.5F);

        double speed = this.vehicle.getDeltaMovement().length();
        this.pitch = Mth.clamp(0.94F + (float) speed * 0.14F, 0.9F, 1.15F);
    }

    /** Public hear range for start-gate checks (must match instance falloff). */
    public static float hearDistanceBlocks(VehicleEntity vehicle) {
        String id = vehicle.vehicleDataId().getPath();
        if ("lav150".equals(id) || "bmp2".equals(id)) {
            return GUN_FIRE_RANGE_BLOCKS * 0.85F;
        }
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        float factor = switch (type) {
            case HELICOPTER, AIRCRAFT -> 1.25F;
            case LAND, BOAT, ARTILLERY -> 0.5F;
        };
        return GUN_FIRE_RANGE_BLOCKS * factor;
    }

    private float maxHearDistance() {
        return hearDistanceBlocks(this.vehicle);
    }

    private void updatePosition() {
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY() + this.vehicle.getBbHeight() * 0.35D;
        this.z = this.vehicle.getZ();
    }
}
