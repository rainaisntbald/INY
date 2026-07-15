package net.iridiummc.iny.exception;

/** Raised when a required path segment does not exist. */
public final class InyMissingValueException extends InyPathException {

    /** Missing path segment. */
    private final String missingSegment;
    /** Zero-based index of the missing segment. */
    private final int segmentIndex;
    /** Whether the missing segment is the final path segment. */
    private final boolean finalSegment;

    /**
     * Creates a missing-path-value failure.
     *
     * @param path complete path being resolved
     * @param missingSegment missing path segment
     * @param segmentIndex zero-based index of the missing segment
     * @param finalSegment whether the missing segment is the final segment
     */
    public InyMissingValueException(String path, String missingSegment, int segmentIndex, boolean finalSegment) {
        super("Missing " + (finalSegment ? "final" : "intermediate") + " value '" + missingSegment
                + "' while resolving INY path '" + path + "'", path);
        this.missingSegment = missingSegment;
        this.segmentIndex = segmentIndex;
        this.finalSegment = finalSegment;
    }

    /**
     * Returns the missing path segment.
     *
     * @return missing segment
     */
    public String missingSegment() {
        return missingSegment;
    }

    /**
     * Returns the position of the missing path segment.
     *
     * @return zero-based segment index
     */
    public int segmentIndex() {
        return segmentIndex;
    }

    /**
     * Returns whether the missing segment was the final segment.
     *
     * @return {@code true} for a missing final segment
     */
    public boolean finalSegment() {
        return finalSegment;
    }
}
