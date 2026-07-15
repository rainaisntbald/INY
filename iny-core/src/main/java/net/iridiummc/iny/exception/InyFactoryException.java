package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Base class for path-aware factory resolution failures. */
public class InyFactoryException extends InyException {

    /** Configuration path containing the call. */
    private final String path;
    /** Factory identifier. */
    private final InyIdentifier identifier;

    /**
     * Creates a path-aware factory failure.
     *
     * @param message diagnostic message
     * @param path configuration path containing the call
     * @param identifier factory identifier
     */
    protected InyFactoryException(String message, String path, InyIdentifier identifier) {
        super(message);
        this.path = Objects.requireNonNull(path, "path");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Creates a path-aware factory failure with an underlying cause.
     *
     * @param message diagnostic message
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param cause underlying failure
     */
    protected InyFactoryException(String message, String path, InyIdentifier identifier, Throwable cause) {
        super(message, cause);
        this.path = Objects.requireNonNull(path, "path");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Returns the configuration path containing the call.
     *
     * @return configuration path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the factory identifier.
     *
     * @return factory identifier
     */
    public InyIdentifier identifier() {
        return identifier;
    }
}
