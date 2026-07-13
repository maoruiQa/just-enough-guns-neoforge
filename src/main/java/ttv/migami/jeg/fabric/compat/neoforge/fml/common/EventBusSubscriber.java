package ttv.migami.jeg.fabric.compat.neoforge.fml.common;

import ttv.migami.jeg.fabric.compat.neoforge.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid();
    Dist[] value() default {};
}
