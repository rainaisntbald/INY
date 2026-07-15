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

    public Optional<InyFactoryRegistration<?>> find(InyIdentifier identifier) {
        return Optional.ofNullable(registrations.get(Objects.requireNonNull(identifier, "identifier")));
    }

    public boolean contains(InyIdentifier identifier) {
        return registrations.containsKey(Objects.requireNonNull(identifier, "identifier"));
    }

    public int size() {
        return registrations.size();
    }

    /** Returns the immutable insertion-ordered registration snapshot. */
    public Map<InyIdentifier, InyFactoryRegistration<?>> registrations() {
        return registrations;
    }
}
