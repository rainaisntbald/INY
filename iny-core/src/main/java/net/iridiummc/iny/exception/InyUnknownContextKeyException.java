package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when configuration refers to a context key not registered with the INY service. */
public final class InyUnknownContextKeyException extends InyException {

    /** Unregistered context-key identifier. */
    private final InyIdentifier identifier;

    /**
     * Creates an unknown context-key failure.
     *
     * @param identifier unregistered key identifier
     */
    public InyUnknownContextKeyException(InyIdentifier identifier) {
        super("No runtime context key is registered for " + identifier);
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Returns the unknown key identifier.
     *
     * @return unregistered key identifier
     */
    public InyIdentifier identifier() {
        return identifier;
    }
}
