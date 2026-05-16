package ttv.migami.jeg.vehicle.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleFireSoundInstance extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 6.5F;
    private final VehicleEntity vehicle;

    public VehicleFireSoundInstance(VehicleEntity vehicle, SoundEvent sound) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.updatePosition();
    }

    @Override
    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.isAlive() || !this.vehicle.isWeaponFiring()) {
            this.stop();
            return;
        }
        this.updatePosition();
        double speed = this.vehicle.getDeltaMovement().length();
        this.volume = Mth.clamp(3.5F + (float) speed * 4.0F, 3.5F, MAX_VOLUME);
        this.pitch = Mth.clamp(0.96F + (float) speed * 0.08F, 0.96F, 1.08F);
    }

    private void updatePosition() {
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
    }
}
