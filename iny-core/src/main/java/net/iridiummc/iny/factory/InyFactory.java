package net.iridiummc.iny.factory;

/**
 * Constructs one arbitrary Java object from an unevaluated INY call.
 *
 * @param <T> object type produced by the factory
 */
@FunctionalInterface
public interface InyFactory<T> {

    /**
     * Creates a value from one call and its arguments.
     *
     * @param context context for the current factory invocation
     * @return the created non-null value
     */
    T create(InyFactoryContext context);
}
