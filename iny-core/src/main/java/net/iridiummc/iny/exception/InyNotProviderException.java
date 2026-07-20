package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when a runnable-only value is requested as a value provider. */
public final class InyNotProviderException extends InyException {

    /** Configuration or argument path. */
    private final String path;
    /** Requested provider result type. */
    private final Class<?> requestedResultType;
    /** Actual runnable type. */
    private final Class<?> actualType;

    /**
     * Creates a runnable/provider mismatch failure.
     *
     * @param path configuration or argument path
     * @param requestedResultType requested provider result type
     * @param actualType actual runnable type
     */
    public InyNotProviderException(String path, Class<?> requestedResultType, Class<?> actualType) {
        super("Runtime value at path '" + path + "' is " + actualType.getTypeName()
                + ", which is runnable but does not provide " + requestedResultType.getTypeName());
        this.path = Objects.requireNonNull(path, "path");
        this.requestedResultType = Objects.requireNonNull(requestedResultType, "requestedResultType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
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
     * Returns the requested provider result type.
     *
     * @return requested result type
     */
    public Class<?> requestedResultType() {
        return requestedResultType;
    }

    /**
     * Returns the actual runnable type.
     *
     * @return actual Java type
     */
    public Class<?> actualType() {
        return actualType;
    }
}
