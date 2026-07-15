package net.iridiummc.iny.exception;

import net.iridiummc.iny.value.InyValueType;

/** Raised when path navigation attempts to traverse a non-section value. */
public final class InyPathTraversalException extends InyPathException {

    private final String segment;
    private final InyValueType actualType;

    public InyPathTraversalException(String path, String segment, InyValueType actualType) {
        super("Cannot traverse INY path '" + path + "' through segment '" + segment
                + "': it is " + actualType + ", not a section", path);
        this.segment = segment;
        this.actualType = actualType;
    }

    public String segment() {
        return segment;
    }

    public InyValueType actualType() {
        return actualType;
    }
}
