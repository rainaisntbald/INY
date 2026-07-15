package net.iridiummc.iny.exception;

import net.iridiummc.iny.api.InyIdentifier;

/** Raised when a call supplies a factory with an unsupported number of arguments. */
public final class InyArgumentCountException extends InyFactoryException {

    private final int minimum;
    private final int maximum;
    private final int actual;

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

    public int minimum() {
        return minimum;
    }

    public int maximum() {
        return maximum;
    }

    public int actual() {
        return actual;
    }
}
