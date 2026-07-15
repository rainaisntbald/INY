package net.iridiummc.iny.codec;

import net.iridiummc.iny.value.InyValue;

/**
 * Converts one semantic INY value into a requested Java type.
 *
 * @param <T> exact Java target type
 */
public interface InyDecoder<T> {

    /** Returns the exact class literal used as this decoder's registry key. */
    Class<T> targetType();

    /** Decodes a value or throws a contextual decoding exception. */
    T decode(InyValue value, InyDecodeContext context);
}
