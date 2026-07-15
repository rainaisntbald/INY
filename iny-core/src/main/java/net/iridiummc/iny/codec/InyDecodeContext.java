package net.iridiummc.iny.codec;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.exception.InyDecodeException;

/** Path-aware operations supplied to a custom decoder. */
public interface InyDecodeContext {

    /**
     * Returns the owning immutable INY service.
     *
     * @return the owning service
     */
    Iny service();

    /**
     * Returns the complete configuration path currently being decoded.
     *
     * @return the current configuration path
     */
    String path();

    /**
     * Returns the Java type requested for the current value.
     *
     * @return the requested Java type
     */
    Class<?> requestedType();

    /**
     * Decodes another ordinary Java value at the same logical path.
     *
     * @param childValue ordinary Java value to decode
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded value
     */
    <T> T decode(Object childValue, Class<T> type);

    /**
     * Decodes a named child while extending this context's dotted path.
     *
     * @param childName child path segment
     * @param childValue ordinary Java value to decode
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded child value
     */
    <T> T decodeChild(String childName, Object childValue, Class<T> type);

    /**
     * Creates a decoding failure populated with the current path, target, and Java-side value type.
     *
     * @param explanation explanation of why decoding failed
     * @return a contextual decoding exception
     */
    InyDecodeException failure(String explanation);

    /**
     * Creates a decoding failure populated with current context and a cause.
     *
     * @param explanation explanation of why decoding failed
     * @param cause underlying failure
     * @return a contextual decoding exception
     */
    InyDecodeException failure(String explanation, Throwable cause);
}
