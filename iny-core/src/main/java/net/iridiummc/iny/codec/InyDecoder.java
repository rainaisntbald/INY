package net.iridiummc.iny.codec;

/**
 * Converts an ordinary Java representation of an INY value into a requested Java type.
 *
 * @param <T> exact Java target type
 */
public interface InyDecoder<T> {

    /**
     * Returns the exact class literal used as this decoder's registry key.
     *
     * @return the exact target type
     */
    Class<T> targetType();

    /**
     * Decodes a value or throws a contextual decoding exception.
     *
     * @param value ordinary Java representation to decode
     * @param context path-aware decoding context
     * @return the decoded value
     */
    T decode(Object value, InyDecodeContext context);
}
