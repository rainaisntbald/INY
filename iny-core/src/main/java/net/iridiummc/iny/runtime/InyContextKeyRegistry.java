package net.iridiummc.iny.runtime;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyUnknownContextKeyException;
import net.iridiummc.iny.internal.runtime.ImmutableInyContextKeyRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable registry describing the typed runtime context keys known to an INY service. */
public interface InyContextKeyRegistry {

    /** Creates an immutable registry snapshot from identifier-key entries. */
    static InyContextKeyRegistry copyOf(Map<InyIdentifier, InyContextKey<?>> entries) {
        return new ImmutableInyContextKeyRegistry(entries);
    }

    /** Finds a registered context key. */
    Optional<InyContextKey<?>> find(InyIdentifier identifier);

    /** Returns a registered context key or throws a contextual INY failure. */
    default InyContextKey<?> require(InyIdentifier identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return find(identifier).orElseThrow(() -> new InyUnknownContextKeyException(identifier));
    }

    /** Returns the immutable insertion-ordered registry entries. */
    Map<InyIdentifier, InyContextKey<?>> entries();
}
