package net.iridiummc.iny.codec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable exact-type decoder registry, safe for concurrent reads. */
public final class InyDecoderRegistry {

    private final Map<Class<?>, InyDecoder<?>> decoders;

    /**
     * Creates an immutable snapshot of decoder registrations.
     *
     * @param decoders decoders keyed by their exact target type
     */
    public InyDecoderRegistry(Map<Class<?>, InyDecoder<?>> decoders) {
        Objects.requireNonNull(decoders, "decoders");
        LinkedHashMap<Class<?>, InyDecoder<?>> copy = new LinkedHashMap<>();
        decoders.forEach((type, decoder) -> {
            Objects.requireNonNull(type, "decoder type");
            Objects.requireNonNull(decoder, "decoder");
            if (!type.equals(decoder.targetType())) {
                throw new IllegalArgumentException("Decoder map key " + type.getTypeName()
                        + " does not match decoder target " + decoder.targetType().getTypeName());
            }
            copy.put(type, decoder);
        });
        this.decoders = Collections.unmodifiableMap(copy);
    }

    /**
     * Finds the decoder registered for an exact target type.
     *
     * @param targetType exact target type
     * @param <T> target type
     * @return the matching decoder, or empty when none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<InyDecoder<T>> find(Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        return Optional.ofNullable((InyDecoder<T>) decoders.get(targetType));
    }

    /**
     * Tests whether an exact target type has a registered decoder.
     *
     * @param targetType exact target type
     * @return {@code true} when a decoder is registered
     */
    public boolean contains(Class<?> targetType) {
        return decoders.containsKey(Objects.requireNonNull(targetType, "targetType"));
    }

    /**
     * Returns the number of registered decoders.
     *
     * @return decoder count
     */
    public int size() {
        return decoders.size();
    }
}
