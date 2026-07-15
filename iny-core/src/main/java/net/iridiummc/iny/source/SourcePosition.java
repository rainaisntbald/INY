package net.iridiummc.iny.source;

import java.util.Objects;

/** A zero-based character offset and one-based line and column in an INY source. */
public record SourcePosition(String sourceName, int offset, int line, int column) {

    public SourcePosition {
        Objects.requireNonNull(sourceName, "sourceName");
        if (offset < 0 || line < 1 || column < 1) {
            throw new IllegalArgumentException("Invalid source position");
        }
    }

    @Override
    public String toString() {
        return sourceName + ":" + line + ":" + column;
    }
}
