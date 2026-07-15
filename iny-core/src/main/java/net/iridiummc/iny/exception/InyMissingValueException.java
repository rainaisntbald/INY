package net.iridiummc.iny.exception;

/** Raised when a required path segment does not exist. */
public final class InyMissingValueException extends InyPathException {

    private final String missingSegment;
    private final int segmentIndex;
    private final boolean finalSegment;

    public InyMissingValueException(String path, String missingSegment, int segmentIndex, boolean finalSegment) {
        super("Missing " + (finalSegment ? "final" : "intermediate") + " value '" + missingSegment
                + "' while resolving INY path '" + path + "'", path);
        this.missingSegment = missingSegment;
        this.segmentIndex = segmentIndex;
        this.finalSegment = finalSegment;
    }

    public String missingSegment() {
        return missingSegment;
    }

    public int segmentIndex() {
        return segmentIndex;
    }

    public boolean finalSegment() {
        return finalSegment;
    }
}
