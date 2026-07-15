package net.iridiummc.iny.factory;

/** Constructs one arbitrary Java object from an unevaluated INY call. */
@FunctionalInterface
public interface InyFactory<T> {

    T create(InyFactoryContext context);
}
