package net.iridiummc.iny.codec;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.exception.InyDecodeException;
import net.iridiummc.iny.value.InyValue;

import java.util.Objects;

/** Context supplied to decoders for nested decoding and path-aware failures. */
public final class InyDecodeContext {

    private final Iny service;
    private final String path;
    private final Class<?> requestedType;
    private final InyValue value;

    public InyDecodeContext(Iny service, String path, Class<?> requestedType, InyValue value) {
        this.service = Objects.requireNonNull(service, "service");
        this.path = Objects.requireNonNull(path, "path");
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.value = Objects.requireNonNull(value, "value");
    }

    /** Returns the owning immutable INY service. */
    public Iny service() {
        return service;
    }

    /** Returns the complete configuration path currently being decoded. */
    public String path() {
        return path;
    }

    /** Returns the Java type requested for the current value. */
    public Class<?> requestedType() {
        return requestedType;
    }

    /** Decodes another value at the same logical path. */
    public <T> T decode(InyValue childValue, Class<T> type) {
        return service.decodeValue(childValue, type, path);
    }

    /** Decodes a named child while extending this context's dotted path. */
    public <T> T decodeChild(String childName, InyValue childValue, Class<T> type) {
        Objects.requireNonNull(childName, "childName");
        String childPath = path.isEmpty() ? childName
                : childName.startsWith("[") ? path + childName : path + "." + childName;
        return service.decodeValue(childValue, type, childPath);
    }

    /** Creates a decoding failure populated with the current path, target, and value kind. */
    public InyDecodeException failure(String explanation) {
        return new InyDecodeException(path, requestedType, value.type(), explanation);
    }

    /** Creates a decoding failure populated with current context and a cause. */
    public InyDecodeException failure(String explanation, Throwable cause) {
        return new InyDecodeException(path, requestedType, value.type(), explanation, cause);
    }
}
