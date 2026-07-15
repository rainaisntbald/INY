package net.iridiummc.iny.api;

/** Lightweight grouping of related factory and decoder registrations. */
@FunctionalInterface
public interface InyModule {

    /**
     * Adds this module's registrations to a service builder.
     *
     * @param builder builder to configure
     */
    void configure(Iny.Builder builder);
}
