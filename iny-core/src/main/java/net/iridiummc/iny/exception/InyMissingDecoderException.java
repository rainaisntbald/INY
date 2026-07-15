package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when no decoder is registered for a requested Java type. */
public final class InyMissingDecoderException extends InyException {

    /** Configuration path being decoded. */
    private final String path;
    /** Java target type without a decoder. */
    private final Class<?> targetType;

    /**
     * Creates a missing-decoder failure.
     *
     * @param path configuration path being decoded
     * @param targetType requested Java type
     */
    public InyMissingDecoderException(String path, Class<?> targetType) {
        super("No INY decoder is registered for " + targetType.getTypeName() + " at path '" + path + "'");
        this.path = Objects.requireNonNull(path, "path");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    /**
     * Returns the configuration path being decoded.
     *
     * @return configuration path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the unregistered target type.
     *
     * @return requested Java type
     */
    public Class<?> targetType() {
        return targetType;
    }
}
