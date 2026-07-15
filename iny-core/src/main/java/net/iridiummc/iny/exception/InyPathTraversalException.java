package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when path navigation attempts to traverse a non-section value. */
public final class InyPathTraversalException extends InyPathException {

    /** Non-section segment through which traversal was attempted. */
    private final String segment;
    /** Human-readable value type found at the segment. */
    private final String actualType;

    /**
     * Creates a non-section traversal failure.
     *
     * @param path complete path being resolved
     * @param segment non-section path segment
     * @param actualType human-readable value type found at the segment
     */
    public InyPathTraversalException(String path, String segment, String actualType) {
        super("Cannot traverse INY path '" + path + "' through segment '" + segment
                + "': it is " + actualType + ", not a section", path);
        this.segment = Objects.requireNonNull(segment, "segment");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    /**
     * Returns the segment through which traversal was attempted.
     *
     * @return non-section segment
     */
    public String segment() {
        return segment;
    }

    /**
     * Returns the human-readable value type found at the segment.
     *
     * @return actual value type
     */
    public String actualType() {
        return actualType;
    }
}
