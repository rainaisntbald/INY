package net.iridiummc.iny.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a method as an explicitly discovered INY factory. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InyFactory {

    /**
     * Returns the canonical namespaced identifier used to register the factory.
     *
     * @return INY factory identifier
     */
    String value();
}
