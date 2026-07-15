package net.iridiummc.iny.exception;

import java.nio.file.Path;
import java.util.Objects;

/** Raised when source text cannot be read. */
public final class InySourceLoadException extends InyException {

    /** Diagnostic name of the source that could not be loaded. */
    private final String sourceName;

    /**
     * Creates a source-loading failure for a named source.
     *
     * @param sourceName diagnostic source name
     * @param cause underlying input failure
     */
    public InySourceLoadException(String sourceName, Throwable cause) {
        super("Could not load INY source '" + sourceName + "': " + cause.getMessage(), cause);
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
    }

    /**
     * Creates a source-loading failure for a file.
     *
     * @param path file that could not be loaded
     * @param cause underlying input failure
     */
    public InySourceLoadException(Path path, Throwable cause) {
        this(path.toString(), cause);
    }

    /**
     * Returns the source name used in diagnostics.
     *
     * @return diagnostic source name
     */
    public String sourceName() {
        return sourceName;
    }
}
