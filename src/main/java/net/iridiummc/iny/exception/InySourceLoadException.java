package net.iridiummc.iny.exception;

import java.nio.file.Path;
import java.util.Objects;

/** Raised when source text cannot be read. */
public final class InySourceLoadException extends InyException {

    private final String sourceName;

    public InySourceLoadException(String sourceName, Throwable cause) {
        super("Could not load INY source '" + sourceName + "': " + cause.getMessage(), cause);
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
    }

    public InySourceLoadException(Path path, Throwable cause) {
        this(path.toString(), cause);
    }

    public String sourceName() {
        return sourceName;
    }
}
