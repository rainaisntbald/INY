package net.iridiummc.iny.exception;

import java.util.Objects;

/** Base class for invalid or unsuccessful dotted-path navigation. */
public abstract class InyPathException extends InyException {

    private final String path;

    protected InyPathException(String message, String path) {
        super(message);
        this.path = Objects.requireNonNull(path, "path");
    }

    public String path() {
        return path;
    }
}
