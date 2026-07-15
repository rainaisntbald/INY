package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

/** Raised when a call supplies a factory with an unsupported number of arguments. */
public final class InyArgumentCountException extends InyFactoryException {

    /** Minimum accepted argument count. */
    private final int minimum;
    /** Maximum accepted argument count. */
    private final int maximum;
    /** Supplied argument count. */
    private final int actual;

    /**
     * Creates an argument-count failure.
     *
     * @param path configuration path containing the call
     * @param identifier factory identifier
     * @param minimum minimum accepted argument count
     * @param maximum maximum accepted argument count
     * @param actual supplied argument count
     */
    public InyArgumentCountException(
            String path,
            InyIdentifier identifier,
            int minimum,
            int maximum,
            int actual
    ) {
        super("Invalid argument count for " + identifier + " at path '" + path + "': expected "
                + expected(minimum, maximum) + " but found " + actual, path, identifier);
        this.minimum = minimum;
        this.maximum = maximum;
        this.actual = actual;
    }

    private static String expected(int minimum, int maximum) {
        return minimum == maximum ? Integer.toString(minimum) : minimum + " to " + maximum;
    }

    /**
     * Returns the minimum accepted argument count.
     *
     * @return the minimum accepted argument count
     */
    public int minimum() {
        return minimum;
    }

    /**
     * Returns the maximum accepted argument count.
     *
     * @return the maximum accepted argument count
     */
    public int maximum() {
        return maximum;
    }

    /**
     * Returns the supplied argument count.
     *
     * @return the supplied argument count
     */
    public int actual() {
        return actual;
    }
}
