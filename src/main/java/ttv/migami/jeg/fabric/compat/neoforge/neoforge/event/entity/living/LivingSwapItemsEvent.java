package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;

public final class LivingSwapItemsEvent {
    private LivingSwapItemsEvent() {
    }

    public static class Hands {
        private final LivingEntity entity;
        private boolean canceled;

        public Hands(LivingEntity entity) {
            this.entity = entity;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public boolean isCanceled() {
            return canceled;
        }
    }
}
