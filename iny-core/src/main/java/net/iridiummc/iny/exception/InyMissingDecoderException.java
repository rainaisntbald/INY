package net.iridiummc.iny.exception;

import java.util.Objects;

/** Raised when no decoder is registered for a requested Java type. */
public final class InyMissingDecoderException extends InyException {

    private final String path;
    private final Class<?> targetType;

    public InyMissingDecoderException(String path, Class<?> targetType) {
        super("No INY decoder is registered for " + targetType.getTypeName() + " at path '" + path + "'");
        this.path = Objects.requireNonNull(path, "path");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    public String path() {
        return path;
    }

    public Class<?> targetType() {
        return targetType;
    }
}
