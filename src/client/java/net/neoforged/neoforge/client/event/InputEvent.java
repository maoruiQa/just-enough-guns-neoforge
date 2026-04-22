package net.neoforged.neoforge.client.event;

import net.minecraft.world.InteractionHand;

public final class InputEvent {
    private InputEvent() {
    }

    public static class InteractionKeyMappingTriggered {
        private final InteractionHand hand;
        private final boolean useItem;
        private boolean swingHand = true;

        public InteractionKeyMappingTriggered(InteractionHand hand, boolean useItem) {
            this.hand = hand;
            this.useItem = useItem;
        }

        public InteractionHand getHand() {
            return hand;
        }

        public boolean isUseItem() {
            return useItem;
        }

        public void setSwingHand(boolean swingHand) {
            this.swingHand = swingHand;
        }

        public boolean isSwingHand() {
            return swingHand;
        }
    }

    public static final class MouseButton {
        private MouseButton() {
        }

        public static final class Post {
            private final int button;
            private final int action;

            public Post(int button, int action) {
                this.button = button;
                this.action = action;
            }

            public int getButton() {
                return button;
            }

            public int getAction() {
                return action;
            }
        }
    }
}
