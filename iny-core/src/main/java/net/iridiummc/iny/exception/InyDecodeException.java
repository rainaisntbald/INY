package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when an existing INY value cannot be converted to the requested Java type. */
public final class InyDecodeException extends InyException {

    /** Configuration path that failed decoding. */
    private final String path;
    /** Requested Java target type. */
    private final Class<?> targetType;
    /** Human-readable source value type. */
    private final String actualType;

    /**
     * Creates a decoding failure.
     *
     * @param path configuration path being decoded
     * @param targetType requested Java type
     * @param actualType human-readable source value type
     * @param explanation explanation of the failed conversion
     */
    public InyDecodeException(String path, Class<?> targetType, String actualType, String explanation) {
        super("Cannot decode INY value at path '" + path + "' as " + targetType.getTypeName()
                + ": actual type is " + actualType + "; " + explanation);
        this.path = Objects.requireNonNull(path, "path");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    /**
     * Creates a decoding failure with an underlying cause.
     *
     * @param path configuration path being decoded
     * @param targetType requested Java type
     * @param actualType human-readable source value type
     * @param explanation explanation of the failed conversion
     * @param cause underlying failure
     */
    public InyDecodeException(
            String path,
            Class<?> targetType,
            String actualType,
            String explanation,
            Throwable cause
    ) {
        super("Cannot decode INY value at path '" + path + "' as " + targetType.getTypeName()
                + ": actual type is " + actualType + "; " + explanation, cause);
        this.path = Objects.requireNonNull(path, "path");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    /**
     * Returns the configuration path that failed decoding.
     *
     * @return failing configuration path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the requested decoding target.
     *
     * @return requested Java type
     */
    public Class<?> targetType() {
        return targetType;
    }

    /**
     * Returns the human-readable source value type.
     *
     * @return actual source value type
     */
    public String actualType() {
        return actualType;
    }
}
