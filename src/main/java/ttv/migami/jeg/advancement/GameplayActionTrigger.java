package ttv.migami.jeg.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;

/** Successful server actions; the vanilla advancement store owns all progress. */
public final class GameplayActionTrigger extends SimpleCriterionTrigger<GameplayActionTrigger.Conditions> {
    @Override public Codec<Conditions> codec() { return Conditions.CODEC; }

    public void fire(ServerPlayer player, String action, String subject) {
        if (!player.isSpectator() && !player.isCreative()) {
            trigger(player, condition -> condition.action().equals(action)
                    && (condition.subject().isEmpty() || condition.subject().get().equals(subject)));
        }
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String action, Optional<String> subject)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Codec.STRING.fieldOf("action").forGetter(Conditions::action),
                Codec.STRING.optionalFieldOf("subject").forGetter(Conditions::subject)
        ).apply(instance, Conditions::new));
    }
}
