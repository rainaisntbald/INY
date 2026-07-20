package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when a provider returns null or a value incompatible with its requested result type. */
public final class InyInvalidProviderResultException extends InyException {

    private final String path;
    private final Class<?> expectedType;
    private final Class<?> actualType;

    /** Creates an invalid provider-result failure. */
    public InyInvalidProviderResultException(
            String path,
            Class<?> expectedType,
            Object actualResult
    ) {
        this(path, expectedType, actualResult, null);
    }

    /** Creates an invalid provider-result failure retaining an underlying cause. */
    public InyInvalidProviderResultException(
            String path,
            Class<?> expectedType,
            Object actualResult,
            Throwable cause
    ) {
        super(message(path, expectedType, actualResult), cause);
        this.path = Objects.requireNonNull(path, "path");
        this.expectedType = Objects.requireNonNull(expectedType, "expectedType");
        this.actualType = actualResult == null ? null : actualResult.getClass();
    }

    private static String message(String path, Class<?> expectedType, Object result) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(expectedType, "expectedType");
        String actual = result == null ? "null" : result.getClass().getTypeName();
        return "Provider at path '" + path + "' returned " + actual + ", but "
                + expectedType.getTypeName() + " was requested";
    }

    /** Returns the configuration or argument path. */
    public String path() {
        return path;
    }

    /** Returns the requested result type. */
    public Class<?> expectedType() {
        return expectedType;
    }

    /** Returns the actual result type, or null when the result was null. */
    public Class<?> actualType() {
        return actualType;
    }
}
