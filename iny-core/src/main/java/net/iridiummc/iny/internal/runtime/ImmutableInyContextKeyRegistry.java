package net.iridiummc.iny.internal.runtime;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyContextKeyRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Internal immutable context-key registry implementation. */
public final class ImmutableInyContextKeyRegistry implements InyContextKeyRegistry {

    private final Map<InyIdentifier, InyContextKey<?>> entries;

    public ImmutableInyContextKeyRegistry(Map<InyIdentifier, InyContextKey<?>> entries) {
        Objects.requireNonNull(entries, "entries");
        LinkedHashMap<InyIdentifier, InyContextKey<?>> copy = new LinkedHashMap<>();
        entries.forEach((identifier, key) -> {
            Objects.requireNonNull(identifier, "context key identifier");
            Objects.requireNonNull(key, "context key");
            if (!identifier.equals(key.identifier())) {
                throw new IllegalArgumentException("Context key map identifier " + identifier
                        + " does not match key identifier " + key.identifier());
            }
            InyContextKey<?> existing = copy.putIfAbsent(identifier, key);
            if (existing != null && !existing.type().equals(key.type())) {
                throw incompatible(identifier, existing.type(), key.type());
            }
        });
        this.entries = Collections.unmodifiableMap(copy);
    }

    @Override
    public Optional<InyContextKey<?>> find(InyIdentifier identifier) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(identifier, "identifier")));
    }

    @Override
    public Map<InyIdentifier, InyContextKey<?>> entries() {
        return entries;
    }

    public static IllegalArgumentException incompatible(
            InyIdentifier identifier,
            Class<?> existingType,
            Class<?> newType
    ) {
        return new IllegalArgumentException("Context key '" + identifier + "' is already registered as "
                + existingType.getTypeName() + " and cannot also use " + newType.getTypeName());
    }
}
