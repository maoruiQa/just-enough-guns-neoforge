package ttv.migami.jeg.animations;

import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.constant.DefaultAnimations;
import ttv.migami.jeg.entity.monster.phantom.terror.TerrorPhantom;

public class TerrorPhantomAnimations {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation ROLL = RawAnimation.begin().thenPlay("roll").thenLoop("idle");
    public static final RawAnimation DYING = RawAnimation.begin().thenPlay("dying");

    /**
     * Generic {@link DefaultAnimations#WALK walk} + {@link DefaultAnimations#IDLE idle} controller.<br>
     * Will play the walk animation if the animatable is considered moving, or idle if not
     */
    public static <T extends GeoAnimatable> AnimationController<TerrorPhantom> genericIdleController(TerrorPhantom animatable) {
        return new AnimationController<>(animatable, "Idle", 0, state -> {
            if (animatable.isDying()) {
                return state.setAndContinue(DYING);
            }
            else if (animatable.isRolling()) {
                return state.setAndContinue(ROLL);
            }

            state.setAndContinue(IDLE);

            return PlayState.CONTINUE;
        });
    }
}
