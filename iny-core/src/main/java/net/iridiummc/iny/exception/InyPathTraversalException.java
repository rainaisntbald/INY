package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when path navigation attempts to traverse a non-section value. */
public final class InyPathTraversalException extends InyPathException {

    private final String segment;
    private final String actualType;

    public InyPathTraversalException(String path, String segment, String actualType) {
        super("Cannot traverse INY path '" + path + "' through segment '" + segment
                + "': it is " + actualType + ", not a section", path);
        this.segment = Objects.requireNonNull(segment, "segment");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    public String segment() {
        return segment;
    }

    public String actualType() {
        return actualType;
    }
}
