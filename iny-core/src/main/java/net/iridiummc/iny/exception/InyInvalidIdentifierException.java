package net.iridiummc.iny.exception;

/** Raised when a namespaced INY identifier is malformed. */
public final class InyInvalidIdentifierException extends InyException {

    private final String identifier;

    public InyInvalidIdentifierException(String identifier, String explanation) {
        super("Invalid INY identifier '" + identifier + "': " + explanation);
        this.identifier = identifier;
    }

    public String identifier() {
        return identifier;
    }
}
