package net.iridiummc.iny.exception;

/** Raised when a dotted path is empty or contains an invalid segment. */
public final class InyInvalidPathException extends InyPathException {

    /**
     * Creates an invalid-path failure.
     *
     * @param path rejected path text
     * @param explanation violated path rule
     */
    public InyInvalidPathException(String path, String explanation) {
        super("Invalid INY path '" + path + "': " + explanation, path);
    }
}
