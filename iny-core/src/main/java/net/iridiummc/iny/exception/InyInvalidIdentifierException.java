package net.iridiummc.iny.exception;

/** Raised when a namespaced INY identifier is malformed. */
public final class InyInvalidIdentifierException extends InyException {

    /** Rejected identifier text. */
    private final String identifier;

    /**
     * Creates an invalid-identifier failure.
     *
     * @param identifier rejected identifier text
     * @param explanation violated identifier rule
     */
    public InyInvalidIdentifierException(String identifier, String explanation) {
        super("Invalid INY identifier '" + identifier + "': " + explanation);
        this.identifier = identifier;
    }

    /**
     * Returns the rejected identifier text.
     *
     * @return rejected identifier
     */
    public String identifier() {
        return identifier;
    }
}
