package net.iridiummc.iny.exception;

/** Base class for failures produced by the INY subsystem. */
public class InyException extends RuntimeException {

    /**
     * Creates an INY failure with a message.
     *
     * @param message diagnostic message
     */
    public InyException(String message) {
        super(message);
    }

    /**
     * Creates an INY failure with a message and cause.
     *
     * @param message diagnostic message
     * @param cause underlying failure
     */
    public InyException(String message, Throwable cause) {
        super(message, cause);
    }
}
