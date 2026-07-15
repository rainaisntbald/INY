package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when an existing INY value cannot be converted to the requested Java type. */
public final class InyDecodeException extends InyException {

    private final String path;
    private final Class<?> targetType;
    private final String actualType;

    public InyDecodeException(String path, Class<?> targetType, String actualType, String explanation) {
        super("Cannot decode INY value at path '" + path + "' as " + targetType.getTypeName()
                + ": actual type is " + actualType + "; " + explanation);
        this.path = Objects.requireNonNull(path, "path");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

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

    public String path() {
        return path;
    }

    public Class<?> targetType() {
        return targetType;
    }

    public String actualType() {
        return actualType;
    }
}
