package net.iridiummc.iny.exception;

import net.iridiummc.iny.runtime.InyContextKey;

import java.util.Objects;

/** Raised when runtime execution requires a context value that was not supplied. */
public final class InyMissingContextValueException extends InyException {

    /** Required key that was absent. */
    private final InyContextKey<?> key;

    /**
     * Creates a missing runtime value failure.
     *
     * @param key required key that was absent
     */
    public InyMissingContextValueException(InyContextKey<?> key) {
        super(message(key));
        this.key = Objects.requireNonNull(key, "key");
    }

    private static String message(InyContextKey<?> key) {
        Objects.requireNonNull(key, "key");
        return "No runtime context value found for key '" + key.identifier()
                + "' with type " + key.type().getTypeName();
    }

    /**
     * Returns the missing key.
     *
     * @return missing context key
     */
    public InyContextKey<?> key() {
        return key;
    }
}
