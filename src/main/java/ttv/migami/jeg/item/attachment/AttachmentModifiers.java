package ttv.migami.jeg.item.attachment;

public record AttachmentModifiers(
        float aimFovModifier,
        float damageMultiplier,
        float spreadMultiplier,
        float recoilMultiplier,
        float kickMultiplier,
        double adsSpeedMultiplier,
        double adsViewXOffset,
        double adsViewYOffset,
        double adsViewZOffset,
        double magazineCapacityMultiplier,
        double fireSoundRadiusMultiplier,
        boolean silenced,
        boolean explosiveAmmo,
        boolean flashlight,
        boolean laserPointer,
        boolean annoying,
        boolean increasedJamming
) {
    public static final AttachmentModifiers NONE = builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float aimFovModifier = 0.0F;
        private float damageMultiplier = 1.0F;
        private float spreadMultiplier = 1.0F;
        private float recoilMultiplier = 1.0F;
        private float kickMultiplier = 1.0F;
        private double adsSpeedMultiplier = 1.0D;
        private double adsViewXOffset = 0.0D;
        private double adsViewYOffset = 0.0D;
        private double adsViewZOffset = 0.0D;
        private double magazineCapacityMultiplier = 1.0D;
        private double fireSoundRadiusMultiplier = 1.0D;
        private boolean silenced;
        private boolean explosiveAmmo;
        private boolean flashlight;
        private boolean laserPointer;
        private boolean annoying;
        private boolean increasedJamming;

        public Builder aimFovModifier(float value) {
            this.aimFovModifier = value;
            return this;
        }

        public Builder damageMultiplier(float value) {
            this.damageMultiplier = value;
            return this;
        }

        public Builder spreadMultiplier(float value) {
            this.spreadMultiplier = value;
            return this;
        }

        public Builder recoilMultiplier(float value) {
            this.recoilMultiplier = value;
            return this;
        }

        public Builder kickMultiplier(float value) {
            this.kickMultiplier = value;
            return this;
        }

        public Builder adsSpeedMultiplier(double value) {
            this.adsSpeedMultiplier = value;
            return this;
        }

        public Builder adsViewOffset(double x, double y, double z) {
            this.adsViewXOffset = x;
            this.adsViewYOffset = y;
            this.adsViewZOffset = z;
            return this;
        }

        public Builder magazineCapacityMultiplier(double value) {
            this.magazineCapacityMultiplier = value;
            return this;
        }

        public Builder fireSoundRadiusMultiplier(double value) {
            this.fireSoundRadiusMultiplier = value;
            return this;
        }

        public Builder silenced() {
            this.silenced = true;
            return this;
        }

        public Builder explosiveAmmo() {
            this.explosiveAmmo = true;
            return this;
        }

        public Builder flashlight() {
            this.flashlight = true;
            return this;
        }

        public Builder laserPointer() {
            this.laserPointer = true;
            return this;
        }

        public Builder annoying() {
            this.annoying = true;
            return this;
        }

        public Builder increasedJamming() {
            this.increasedJamming = true;
            return this;
        }

        public AttachmentModifiers build() {
            return new AttachmentModifiers(
                    this.aimFovModifier,
                    this.damageMultiplier,
                    this.spreadMultiplier,
                    this.recoilMultiplier,
                    this.kickMultiplier,
                    this.adsSpeedMultiplier,
                    this.adsViewXOffset,
                    this.adsViewYOffset,
                    this.adsViewZOffset,
                    this.magazineCapacityMultiplier,
                    this.fireSoundRadiusMultiplier,
                    this.silenced,
                    this.explosiveAmmo,
                    this.flashlight,
                    this.laserPointer,
                    this.annoying,
                    this.increasedJamming
            );
        }
    }
}
