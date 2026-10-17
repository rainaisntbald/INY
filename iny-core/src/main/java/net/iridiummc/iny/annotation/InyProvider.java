package net.iridiummc.iny.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a method as an explicitly discovered deferred INY provider. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InyProvider {

    /**
     * Returns the canonical namespaced identifier used to register the provider.
     *
     * @return INY provider identifier
     */
    String value();
}
