package net.iridiummc.iny.api;

/** Lightweight grouping of related factory and decoder registrations. */
@FunctionalInterface
public interface InyModule {

    void configure(Iny.Builder builder);
}
