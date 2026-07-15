package net.iridiummc.iny.exception;

import java.util.Objects;

/** Base class for invalid or unsuccessful dotted-path navigation. */
public abstract class InyPathException extends InyException {

    /** Dotted path being resolved. */
    private final String path;

    /**
     * Creates a dotted-path navigation failure.
     *
     * @param message diagnostic message
     * @param path path being resolved
     */
    protected InyPathException(String message, String path) {
        super(message);
        this.path = Objects.requireNonNull(path, "path");
    }

    /**
     * Returns the path being resolved.
     *
     * @return dotted path
     */
    public String path() {
        return path;
    }
}
