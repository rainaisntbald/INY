package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

import java.util.Objects;

/** Raised when a builder receives the same factory identifier twice. */
public final class InyDuplicateFactoryException extends InyException {

    private final InyIdentifier identifier;

    public InyDuplicateFactoryException(InyIdentifier identifier) {
        super("An INY factory is already registered for " + identifier);
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    public InyIdentifier identifier() {
        return identifier;
    }
}
