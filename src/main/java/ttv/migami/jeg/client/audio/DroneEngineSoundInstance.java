package ttv.migami.jeg.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import ttv.migami.jeg.entity.DroneEntity;

/**
 * Continuous looping drone engine.
 * <ul>
 *   <li>FPV control: relative to listener, steady volume (including hands-off hover)</li>
 *   <li>World airborne / idle hover: follows entity with distance falloff</li>
 * </ul>
 */
public final class DroneEngineSoundInstance extends AbstractTickableSoundInstance {
    private static final float FPV_VOLUME = 0.5F;
    private static final float WORLD_SOURCE_VOLUME = 1.6F;
    private static final float WORLD_HEAR_RANGE = 48.0F;
    private static final int FADE_TICKS = 5;

    private final int droneEntityId;
    private boolean fpvMode;
    private int fade = FADE_TICKS;
    private boolean dying;

    public DroneEngineSoundInstance(SoundEvent sound, int droneEntityId, boolean fpvMode) {
        super(sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.droneEntityId = droneEntityId;
        this.fpvMode = fpvMode;
        this.looping = true;
        this.delay = 0;
        // Non-zero so the sound engine actually starts the instance
        this.volume = 0.05F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.NONE;
        this.applyModeFlags();
        this.updatePositionFromEntity();
    }

    public int droneEntityId() {
        return this.droneEntityId;
    }

    public boolean isFpvMode() {
        return this.fpvMode;
    }

    public void setFpvMode(boolean fpvMode) {
        if (this.fpvMode == fpvMode) {
            return;
        }
        this.fpvMode = fpvMode;
        this.applyModeFlags();
    }

    private void applyModeFlags() {
        // FPV: glued to listener. World: positioned on drone (manual falloff).
        this.relative = this.fpvMode;
        if (this.fpvMode) {
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }
    }

    public void beginStop() {
        this.dying = true;
    }

    public void keepAlive() {
        this.dying = false;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            this.stop();
            return;
        }

        Entity entity = minecraft.level.getEntity(this.droneEntityId);
        if (!(entity instanceof DroneEntity drone) || !drone.isAlive() || drone.isRemoved()) {
            this.dying = true;
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

        float fadeFactor = this.fade / (float) FADE_TICKS;
        double speed = entity != null ? entity.getDeltaMovement().length() : 0.0D;
        this.pitch = Mth.clamp(0.95F + (float) speed * 0.12F, 0.9F, 1.15F);

        if (this.fpvMode) {
            // Steady hum while piloting - including pure hover (no WASD)
            this.volume = FPV_VOLUME * fadeFactor;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        } else if (entity != null) {
            this.updatePositionFromEntity();
            double distance = entity.position().distanceTo(player.position());
            float proximity = 1.0F - Mth.clamp((float) (distance / WORLD_HEAR_RANGE), 0.0F, 1.0F);
            float curve = proximity * proximity * (3.0F - 2.0F * proximity);
            this.volume = Mth.clamp(WORLD_SOURCE_VOLUME * curve * fadeFactor, 0.0F, 2.5F);
            if (this.volume < 0.01F && this.fade <= 0) {
                this.stop();
            }
        } else {
            this.volume = 0.0F;
        }
    }

    private void updatePositionFromEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(this.droneEntityId);
        if (entity == null) {
            return;
        }
        this.x = entity.getX();
        this.y = entity.getY() + entity.getBbHeight() * 0.35D;
        this.z = entity.getZ();
    }
}
