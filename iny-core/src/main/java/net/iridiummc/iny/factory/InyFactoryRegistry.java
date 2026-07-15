package net.iridiummc.iny.factory;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable namespaced factory registry, safe for concurrent reads. */
public final class InyFactoryRegistry {

    private final Map<InyIdentifier, InyFactoryRegistration<?>> registrations;

    /**
     * Creates an immutable snapshot of factory registrations.
     *
     * @param registrations registrations keyed by identifier
     */
    public InyFactoryRegistry(Map<InyIdentifier, InyFactoryRegistration<?>> registrations) {
        Objects.requireNonNull(registrations, "registrations");
        LinkedHashMap<InyIdentifier, InyFactoryRegistration<?>> copy = new LinkedHashMap<>();
        registrations.forEach((identifier, registration) -> {
            Objects.requireNonNull(identifier, "factory identifier");
            Objects.requireNonNull(registration, "factory registration");
            if (!identifier.equals(registration.identifier())) {
                throw new IllegalArgumentException("Factory map key " + identifier
                        + " does not match registration identifier " + registration.identifier());
            }
            copy.put(identifier, registration);
        });
        this.registrations = Collections.unmodifiableMap(copy);
    }

    /**
     * Finds a registration by identifier.
     *
     * @param identifier factory identifier
     * @return the matching registration, or empty when none exists
     */
    public Optional<InyFactoryRegistration<?>> find(InyIdentifier identifier) {
        return Optional.ofNullable(registrations.get(Objects.requireNonNull(identifier, "identifier")));
    }

    /**
     * Tests whether an identifier has a registration.
     *
     * @param identifier factory identifier
     * @return {@code true} when a factory is registered
     */
    public boolean contains(InyIdentifier identifier) {
        return registrations.containsKey(Objects.requireNonNull(identifier, "identifier"));
    }

    /**
     * Returns the number of registered factories.
     *
     * @return registration count
     */
    public int size() {
        return registrations.size();
    }

    /**
     * Returns the immutable insertion-ordered registration snapshot.
     *
     * @return immutable registration map
     */
    public Map<InyIdentifier, InyFactoryRegistration<?>> registrations() {
        return registrations;
    }
}
