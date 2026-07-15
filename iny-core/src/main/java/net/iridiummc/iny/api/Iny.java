package net.iridiummc.iny.api;

import net.iridiummc.iny.codec.InyDecodeContext;
import net.iridiummc.iny.codec.InyDecoder;
import net.iridiummc.iny.codec.InyDecoderRegistry;
import net.iridiummc.iny.exception.InyMissingDecoderException;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.exception.InyFactoryExecutionException;
import net.iridiummc.iny.exception.InyFactoryException;
import net.iridiummc.iny.exception.InyFactoryTypeException;
import net.iridiummc.iny.exception.InyInvalidFactoryResultException;
import net.iridiummc.iny.exception.InyUnknownFactoryException;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.factory.InyFactoryRegistry;
import net.iridiummc.iny.internal.codec.BuiltInDecoders;
import net.iridiummc.iny.internal.codec.DefaultInyDecodeContext;
import net.iridiummc.iny.internal.codec.InyValueAccess;
import net.iridiummc.iny.internal.config.InyConfigs;
import net.iridiummc.iny.internal.factory.DefaultInyFactoryContext;
import net.iridiummc.iny.internal.lexer.Lexer;
import net.iridiummc.iny.internal.parser.Parser;
import net.iridiummc.iny.internal.source.SourceLoading;
import net.iridiummc.iny.internal.value.InyBoolean;
import net.iridiummc.iny.internal.value.InyCall;
import net.iridiummc.iny.internal.value.InyDecimal;
import net.iridiummc.iny.internal.value.InyInteger;
import net.iridiummc.iny.internal.value.InyList;
import net.iridiummc.iny.internal.value.InyNull;
import net.iridiummc.iny.internal.value.InySectionValue;
import net.iridiummc.iny.internal.value.InyString;
import net.iridiummc.iny.internal.value.InyValue;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.value.InySection;

import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Low-level immutable parser and decoder service.
 * Server integrations should generally share one lifecycle-owned service instead of building one service per plugin.
 */
public final class Iny {

    private final InyDecoderRegistry decoders;
    private final Supplier<InyFactoryRegistry> factories;
    private final InyValueAccess values = new InyValueAccess() {
        @Override
        public <T> T resolve(Object value, Class<T> type, String path) {
            return resolveValue(value, type, path);
        }

        @Override
        public <T> T decode(Object value, Class<T> type, String path) {
            return decodeValue(value, type, path);
        }
    };

    private Iny(InyDecoderRegistry decoders, InyFactoryRegistry factories) {
        this(decoders, () -> factories);
    }

    private Iny(InyDecoderRegistry decoders, Supplier<InyFactoryRegistry> factories) {
        this.decoders = decoders;
        this.factories = Objects.requireNonNull(factories, "factories");
    }

    /**
     * Creates a low-level builder preloaded with all built-in decoders.
     * This is intended for standalone embedding and library tests; server integrations provide their own shared instance.
     *
     * @return a new service builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses string content using {@code <string>} as its diagnostic source name.
     *
     * @param source source text to parse
     * @return the parsed immutable configuration
     */
    public InyConfig parse(String source) {
        return parse("<string>", source);
    }

    /**
     * Parses string content using the supplied diagnostic source name.
     *
     * @param sourceName source name used in diagnostics
     * @param source source text to parse
     * @return the parsed immutable configuration
     */
    public InyConfig parse(String sourceName, String source) {
        return parse(new InySource(sourceName, source));
    }

    /**
     * Reads and parses a caller-owned reader using {@code <reader>} as its source name.
     *
     * @param reader reader to consume without closing
     * @return the parsed immutable configuration
     */
    public InyConfig parse(Reader reader) {
        return parse("<reader>", reader);
    }

    /**
     * Reads and parses a caller-owned reader without closing it.
     *
     * @param sourceName source name used in diagnostics
     * @param reader reader to consume without closing
     * @return the parsed immutable configuration
     */
    public InyConfig parse(String sourceName, Reader reader) {
        return parse(SourceLoading.read(sourceName, reader));
    }

    /**
     * Synchronously loads and parses a UTF-8 file.
     *
     * @param path file to load
     * @return the parsed immutable configuration
     */
    public InyConfig load(Path path) {
        return parse(SourceLoading.load(path));
    }

    /**
     * Parses an already constructed named source.
     *
     * @param source named source to parse
     * @return the parsed immutable configuration
     */
    public InyConfig parse(InySource source) {
        Objects.requireNonNull(source, "source");
        InySectionValue root = new Parser(source, new Lexer(source).lex()).parse();
        return InyConfigs.create(values, root);
    }

    /**
     * Returns the immutable decoder registry used by this service.
     *
     * @return this service's decoder registry
     */
    public InyDecoderRegistry decoders() {
        return decoders;
    }

    /**
     * Returns the immutable factory registry used by this service.
     *
     * @return the current factory registry snapshot
     */
    public InyFactoryRegistry factories() {
        return Objects.requireNonNull(factories.get(), "factory registry source returned null");
    }

    /**
     * Creates a service sharing this service's decoders while obtaining factory snapshots from a caller-owned source.
     * This advanced hook supports lifecycle-scoped adapters; ordinary standalone services should use {@link #builder()}.
     *
     * @param factoryRegistrySource source of current immutable factory snapshots
     * @return a service backed by the supplied factory registry source
     */
    public Iny withFactoryRegistry(Supplier<InyFactoryRegistry> factoryRegistrySource) {
        return new Iny(decoders, Objects.requireNonNull(factoryRegistrySource, "factoryRegistrySource"));
    }

    /** Resolves calls through the factory registry and ordinary values through decoders. */
    private <T> T resolveValue(Object value, Class<T> type, String path) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
        if (!(value instanceof InyValue internalValue)) {
            return decodeValue(value, type, path);
        }
        if (internalValue instanceof InyCall call) {
            return resolveCall(call, type, path);
        }
        return decodeInternalValue(internalValue, type, path);
    }

    private <T> T resolveCall(InyCall call, Class<T> requestedType, String path) {
        InyFactoryRegistration<?> registration = factories().find(call.identifier())
                .orElseThrow(() -> new InyUnknownFactoryException(path, call.identifier(), requestedType));
        Class<?> declaredType = registration.resultType();
        if (!requestedType.isAssignableFrom(declaredType)) {
            throw new InyFactoryTypeException(path, call.identifier(), requestedType, declaredType);
        }

        Object result;
        try {
            result = registration.factory().create(new DefaultInyFactoryContext(this, values, call, path));
        } catch (InyFactoryException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InyFactoryExecutionException(
                    path, call.identifier(), requestedType, declaredType, exception);
        }

        if (result == null || !declaredType.isInstance(result) || !requestedType.isInstance(result)) {
            throw new InyInvalidFactoryResultException(
                    path, call.identifier(), requestedType, declaredType, result);
        }
        return requestedType.cast(result);
    }

    /**
     * Decodes a value through this service's exact-type registry.
     * Primarily used by configuration lookups and decoder contexts.
     */
    private <T> T decodeValue(Object value, Class<T> type, String path) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
        if (value instanceof InyValue internalValue) {
            return decodeInternalValue(internalValue, type, path);
        }
        return decodeJavaValue(value, type, path, describeJavaType(value));
    }

    private <T> T decodeInternalValue(InyValue value, Class<T> type, String path) {
        Object javaValue = toJavaValue(value, path);
        return decodeJavaValue(javaValue, type, path, describeJavaType(javaValue));
    }

    private <T> T decodeJavaValue(Object value, Class<T> type, String path, String actualType) {
        InyDecoder<T> decoder = decoders.find(type)
                .orElseGet(() -> directDecoder(value, type, path));
        InyDecodeContext context = new DefaultInyDecodeContext(this, values, path, type, actualType);
        T decoded = decoder.decode(value, context);
        if (decoded == null && type != Object.class) {
            throw context.failure("decoder returned Java null");
        }
        return decoded;
    }

    private <T> InyDecoder<T> directDecoder(Object value, Class<T> type, String path) {
        Class<?> boxedType = boxed(type);
        if (value != null && boxedType.isInstance(value)) {
            return new InyDecoder<>() {
                @Override
                public Class<T> targetType() {
                    return type;
                }

                @Override
                @SuppressWarnings("unchecked")
                public T decode(Object ignored, InyDecodeContext context) {
                    return (T) value;
                }
            };
        }
        throw new InyMissingDecoderException(path, type);
    }

    private Object toJavaValue(InyValue value, String path) {
        if (value instanceof InyString string) {
            return string.value();
        }
        if (value instanceof InyBoolean bool) {
            return bool.value();
        }
        if (value instanceof InyInteger integer) {
            return integer.value();
        }
        if (value instanceof InyDecimal decimal) {
            return decimal.value();
        }
        if (value instanceof InyNull) {
            return null;
        }
        if (value instanceof InySectionValue section) {
            return InyConfigs.view(values, section, path);
        }
        if (value instanceof InyList list) {
            ArrayList<Object> values = new ArrayList<>(list.values().size());
            for (int index = 0; index < list.values().size(); index++) {
                values.add(resolveValue(list.values().get(index), Object.class, path + "[" + index + "]"));
            }
            return Collections.unmodifiableList(values);
        }
        if (value instanceof InyCall call) {
            return resolveCall(call, Object.class, path);
        }
        throw new AssertionError("Unknown internal INY value " + value.getClass().getTypeName());
    }

    private static String describeJavaType(Object value) {
        if (value == null) return "null";
        if (value instanceof InySection) return "section";
        if (value instanceof List<?>) return "list";
        return value.getClass().getTypeName();
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    /** Builder for an immutable service and decoder registry. */
    public static final class Builder {

        private final Map<Class<?>, InyDecoder<?>> decoders = new LinkedHashMap<>();
        private final Map<InyIdentifier, InyFactoryRegistration<?>> factories = new LinkedHashMap<>();

        private Builder() {
            BuiltInDecoders.install(decoders);
        }

        /**
         * Registers a decoder, rejecting any decoder already registered for its exact target type.
         *
         * @param decoder decoder to register
         * @param <T> decoder target type
         * @return this builder
         */
        public <T> Builder registerDecoder(InyDecoder<T> decoder) {
            Objects.requireNonNull(decoder, "decoder");
            Class<T> targetType = Objects.requireNonNull(decoder.targetType(), "decoder targetType");
            if (decoders.putIfAbsent(targetType, decoder) != null) {
                throw new IllegalArgumentException("A decoder is already registered for " + targetType.getTypeName());
            }
            return this;
        }

        /**
         * Explicitly replaces an existing decoder; use this when overriding is intentional.
         *
         * @param decoder replacement decoder
         * @param <T> decoder target type
         * @return this builder
         */
        public <T> Builder replaceDecoder(InyDecoder<T> decoder) {
            Objects.requireNonNull(decoder, "decoder");
            Class<T> targetType = Objects.requireNonNull(decoder.targetType(), "decoder targetType");
            if (!decoders.containsKey(targetType)) {
                throw new IllegalArgumentException("No decoder is registered for " + targetType.getTypeName());
            }
            decoders.put(targetType, decoder);
            return this;
        }

        /**
         * Registers a concise lambda factory under a parsed namespaced identifier.
         *
         * @param identifier namespaced factory identifier
         * @param resultType declared factory result type
         * @param factory factory implementation
         * @param <T> factory result type
         * @return this builder
         */
        public <T> Builder registerFactory(
                InyIdentifier identifier,
                Class<T> resultType,
                InyFactory<T> factory
        ) {
            return registerFactory(new InyFactoryRegistration<>(identifier, resultType, factory));
        }

        /**
         * Registers a concise lambda factory using a canonical {@code namespace:name} string.
         *
         * @param identifier canonical factory identifier
         * @param resultType declared factory result type
         * @param factory factory implementation
         * @param <T> factory result type
         * @return this builder
         */
        public <T> Builder registerFactory(String identifier, Class<T> resultType, InyFactory<T> factory) {
            return registerFactory(InyIdentifier.parse(identifier), resultType, factory);
        }

        /**
         * Registers an advanced immutable registration, rejecting duplicate identifiers.
         *
         * @param registration registration to add
         * @return this builder
         */
        public Builder registerFactory(InyFactoryRegistration<?> registration) {
            Objects.requireNonNull(registration, "registration");
            if (factories.putIfAbsent(registration.identifier(), registration) != null) {
                throw new InyDuplicateFactoryException(registration.identifier());
            }
            return this;
        }

        /**
         * Explicitly replaces an existing factory registration.
         *
         * @param identifier identifier of the registration to replace
         * @param resultType declared replacement result type
         * @param factory replacement factory
         * @param <T> factory result type
         * @return this builder
         */
        public <T> Builder replaceFactory(
                InyIdentifier identifier,
                Class<T> resultType,
                InyFactory<T> factory
        ) {
            Objects.requireNonNull(identifier, "identifier");
            if (!factories.containsKey(identifier)) {
                throw new IllegalArgumentException("No INY factory is registered for " + identifier);
            }
            factories.put(identifier, new InyFactoryRegistration<>(identifier, resultType, factory));
            return this;
        }

        /**
         * Explicitly replaces an existing factory using a canonical identifier string.
         *
         * @param identifier canonical identifier of the registration to replace
         * @param resultType declared replacement result type
         * @param factory replacement factory
         * @param <T> factory result type
         * @return this builder
         */
        public <T> Builder replaceFactory(String identifier, Class<T> resultType, InyFactory<T> factory) {
            return replaceFactory(InyIdentifier.parse(identifier), resultType, factory);
        }

        /**
         * Installs a lightweight group of related decoders and factories.
         *
         * @param module module to configure this builder
         * @return this builder
         */
        public Builder install(InyModule module) {
            Objects.requireNonNull(module, "module").configure(this);
            return this;
        }

        /**
         * Builds a service with immutable snapshots of the configured decoders and factories.
         *
         * @return the configured immutable service
         */
        public Iny build() {
            return new Iny(new InyDecoderRegistry(decoders), new InyFactoryRegistry(factories));
        }
    }
}
