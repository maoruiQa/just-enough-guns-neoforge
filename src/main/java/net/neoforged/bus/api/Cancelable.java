package net.neoforged.bus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal replacement for the legacy NeoForge @Cancelable annotation used by older code.
 * The modern event bus exposes richer cancellable primitives, but for legacy handlers we
 * only need the marker behaviour to compile.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cancelable {
}
