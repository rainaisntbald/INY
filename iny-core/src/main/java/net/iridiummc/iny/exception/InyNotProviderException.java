package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when a runnable-only value is requested as a value provider. */
public final class InyNotProviderException extends InyException {

    private final String path;
    private final Class<?> requestedResultType;
    private final Class<?> actualType;

    /** Creates a runnable/provider mismatch failure. */
    public InyNotProviderException(String path, Class<?> requestedResultType, Class<?> actualType) {
        super("Runtime value at path '" + path + "' is " + actualType.getTypeName()
                + ", which is runnable but does not provide " + requestedResultType.getTypeName());
        this.path = Objects.requireNonNull(path, "path");
        this.requestedResultType = Objects.requireNonNull(requestedResultType, "requestedResultType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    /** Returns the configuration or argument path. */
    public String path() {
        return path;
    }

    /** Returns the requested provider result type. */
    public Class<?> requestedResultType() {
        return requestedResultType;
    }

    /** Returns the actual runnable type. */
    public Class<?> actualType() {
        return actualType;
    }
}
