package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Base class for path-aware factory resolution failures. */
public class InyFactoryException extends InyException {

    private final String path;
    private final InyIdentifier identifier;

    protected InyFactoryException(String message, String path, InyIdentifier identifier) {
        super(message);
        this.path = Objects.requireNonNull(path, "path");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    protected InyFactoryException(String message, String path, InyIdentifier identifier, Throwable cause) {
        super(message, cause);
        this.path = Objects.requireNonNull(path, "path");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    public String path() {
        return path;
    }

    public InyIdentifier identifier() {
        return identifier;
    }
}
