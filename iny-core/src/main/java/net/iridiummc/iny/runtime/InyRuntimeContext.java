package net.iridiummc.iny.runtime;

import net.iridiummc.iny.exception.InyMissingContextValueException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** An immutable typed set of values supplied to runtime providers and actions. */
public final class InyRuntimeContext {

    private static final InyRuntimeContext EMPTY = new InyRuntimeContext(Map.of());

    private final Map<InyContextKey<?>, Object> values;

    private InyRuntimeContext(Map<InyContextKey<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    /** Returns the shared empty runtime context. */
    public static InyRuntimeContext empty() {
        return EMPTY;
    }

    /** Creates a mutable builder whose completed contexts are immutable snapshots. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns a required typed value. */
    public <T> T get(InyContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        InyContextKey<?> storedKey = storedKey(key);
        if (storedKey == null) {
            throw new InyMissingContextValueException(key);
        }
        requireSameType(storedKey, key);
        return key.type().cast(values.get(storedKey));
    }

    /** Returns a required value for a wildcard key. */
    public Object getUnchecked(InyContextKey<?> key) {
        return get(capture(Objects.requireNonNull(key, "key")));
    }

    /** Finds a typed value without treating absence as a failure. */
    public <T> Optional<T> find(InyContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        InyContextKey<?> storedKey = storedKey(key);
        if (storedKey == null) {
            return Optional.empty();
        }
        requireSameType(storedKey, key);
        return Optional.of(key.type().cast(values.get(storedKey)));
    }

    /** Tests whether a value exists for the key identifier and compatible registered type. */
    public boolean contains(InyContextKey<?> key) {
        Objects.requireNonNull(key, "key");
        InyContextKey<?> storedKey = storedKey(key);
        if (storedKey == null) {
            return false;
        }
        requireSameType(storedKey, key);
        return true;
    }

    /** Returns a new context containing the supplied value. */
    public <T> InyRuntimeContext with(InyContextKey<T> key, T value) {
        Map<InyContextKey<?>, Object> copy = new HashMap<>(values);
        putChecked(copy, key, value);
        return new InyRuntimeContext(copy);
    }

    private InyContextKey<?> storedKey(InyContextKey<?> key) {
        for (InyContextKey<?> candidate : values.keySet()) {
            if (candidate.identifier().equals(key.identifier())) {
                return candidate;
            }
        }
        return null;
    }

    private static <T> void putChecked(
            Map<InyContextKey<?>, Object> values,
            InyContextKey<T> key,
            T value
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        for (InyContextKey<?> existing : values.keySet()) {
            if (existing.identifier().equals(key.identifier())) {
                requireSameType(existing, key);
                break;
            }
        }
        values.put(key, key.type().cast(value));
    }

    private static void requireSameType(InyContextKey<?> existing, InyContextKey<?> requested) {
        if (!existing.type().equals(requested.type())) {
            throw new IllegalArgumentException("Runtime context key '" + requested.identifier()
                    + "' uses type " + requested.type().getTypeName() + " but was already defined as "
                    + existing.type().getTypeName());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> InyContextKey<T> capture(InyContextKey<?> key) {
        return (InyContextKey<T>) key;
    }

    /** Mutable construction helper for immutable runtime contexts. */
    public static final class Builder {
        private final Map<InyContextKey<?>, Object> values = new HashMap<>();

        /** Adds or replaces a value for a key of the same type. */
        public <T> Builder put(InyContextKey<T> key, T value) {
            putChecked(values, key, value);
            return this;
        }

        /** Builds an immutable snapshot. */
        public InyRuntimeContext build() {
            return values.isEmpty() ? EMPTY : new InyRuntimeContext(values);
        }
    }
}
