package net.iridiummc.iny.internal.codec;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.codec.InyDecodeContext;
import net.iridiummc.iny.exception.InyDecodeException;

import java.util.Objects;

/** Internal immutable decoder context. */
public final class DefaultInyDecodeContext implements InyDecodeContext {

    private final Iny service;
    private final InyValueAccess values;
    private final String path;
    private final Class<?> requestedType;
    private final String actualType;

    public DefaultInyDecodeContext(
            Iny service,
            InyValueAccess values,
            String path,
            Class<?> requestedType,
            String actualType
    ) {
        this.service = Objects.requireNonNull(service, "service");
        this.values = Objects.requireNonNull(values, "values");
        this.path = Objects.requireNonNull(path, "path");
        this.requestedType = Objects.requireNonNull(requestedType, "requestedType");
        this.actualType = Objects.requireNonNull(actualType, "actualType");
    }

    @Override
    public Iny service() {
        return service;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Class<?> requestedType() {
        return requestedType;
    }

    @Override
    public <T> T decode(Object childValue, Class<T> type) {
        return values.decode(childValue, type, path);
    }

    @Override
    public <T> T decodeChild(String childName, Object childValue, Class<T> type) {
        Objects.requireNonNull(childName, "childName");
        String childPath = path.isEmpty() ? childName
                : childName.startsWith("[") ? path + childName : path + "." + childName;
        return values.decode(childValue, type, childPath);
    }

    @Override
    public InyDecodeException failure(String explanation) {
        return new InyDecodeException(path, requestedType, actualType, explanation);
    }

    @Override
    public InyDecodeException failure(String explanation, Throwable cause) {
        return new InyDecodeException(path, requestedType, actualType, explanation, cause);
    }
}
