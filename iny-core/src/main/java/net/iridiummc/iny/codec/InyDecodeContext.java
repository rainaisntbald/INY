package net.iridiummc.iny.codec;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.exception.InyDecodeException;

/** Path-aware operations supplied to a custom decoder. */
public interface InyDecodeContext {

    /** Returns the owning immutable INY service. */
    Iny service();

    /** Returns the complete configuration path currently being decoded. */
    String path();

    /** Returns the Java type requested for the current value. */
    Class<?> requestedType();

    /** Decodes another ordinary Java value at the same logical path. */
    <T> T decode(Object childValue, Class<T> type);

    /** Decodes a named child while extending this context's dotted path. */
    <T> T decodeChild(String childName, Object childValue, Class<T> type);

    /** Creates a decoding failure populated with the current path, target, and value kind. */
    InyDecodeException failure(String explanation);

    /** Creates a decoding failure populated with current context and a cause. */
    InyDecodeException failure(String explanation, Throwable cause);
}
