package net.iridiummc.iny.exception;

/** Base class for failures produced by the INY subsystem. */
public class InyException extends RuntimeException {

    public InyException(String message) {
        super(message);
    }

    public InyException(String message, Throwable cause) {
        super(message, cause);
    }
}
