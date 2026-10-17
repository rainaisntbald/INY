package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when a provider returns null or a value incompatible with its requested result type. */
public final class InyInvalidProviderResultException extends InyException {

    /** Configuration or argument path. */
    private final String path;
    /** Requested provider result type. */
    private final Class<?> expectedType;
    /** Actual result type, or null for a null result. */
    private final Class<?> actualType;

    /**
     * Creates an invalid provider-result failure.
     *
     * @param path configuration or argument path
     * @param expectedType requested result type
     * @param actualResult incompatible result, or null
     */
    public InyInvalidProviderResultException(
            String path,
            Class<?> expectedType,
            Object actualResult
    ) {
        this(path, expectedType, actualResult, null);
    }

    /**
     * Creates an invalid provider-result failure retaining an underlying cause.
     *
     * @param path configuration or argument path
     * @param expectedType requested result type
     * @param actualResult incompatible result, or null
     * @param cause underlying failure
     */
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

    /**
     * Returns the configuration or argument path.
     *
     * @return failing path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the requested result type.
     *
     * @return expected Java type
     */
    public Class<?> expectedType() {
        return expectedType;
    }

    /**
     * Returns the actual result type, or null when the result was null.
     *
     * @return actual Java type, or null
     */
    public Class<?> actualType() {
        return actualType;
    }
}
