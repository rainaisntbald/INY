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
import net.iridiummc.iny.internal.factory.DefaultInyFactoryContext;
import net.iridiummc.iny.internal.lexer.Lexer;
import net.iridiummc.iny.internal.parser.Parser;
import net.iridiummc.iny.internal.source.SourceLoading;
import net.iridiummc.iny.source.InySource;
import net.iridiummc.iny.value.InySection;
import net.iridiummc.iny.value.InyCall;
import net.iridiummc.iny.value.InyValue;

import java.io.Reader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Parses string content using {@code <string>} as its diagnostic source name. */
    public InyConfig parse(String source) {
        return parse("<string>", source);
    }

    /** Parses string content using the supplied diagnostic source name. */
    public InyConfig parse(String sourceName, String source) {
        return parse(new InySource(sourceName, source));
    }

    /** Reads and parses a caller-owned reader using {@code <reader>} as its source name. */
    public InyConfig parse(Reader reader) {
        return parse("<reader>", reader);
    }

    /** Reads and parses a caller-owned reader without closing it. */
    public InyConfig parse(String sourceName, Reader reader) {
        return parse(SourceLoading.read(sourceName, reader));
    }

    /** Synchronously loads and parses a UTF-8 file. */
    public InyConfig load(Path path) {
        return parse(SourceLoading.load(path));
    }

    /** Parses an already constructed named source. */
    public InyConfig parse(InySource source) {
        Objects.requireNonNull(source, "source");
        InySection root = new Parser(source, new Lexer(source).lex()).parse();
        return new InyConfig(this, root);
    }

    /** Returns the immutable decoder registry used by this service. */
    public InyDecoderRegistry decoders() {
        return decoders;
    }

    /** Returns the immutable factory registry used by this service. */
    public InyFactoryRegistry factories() {
        return Objects.requireNonNull(factories.get(), "factory registry source returned null");
    }

    /**
     * Creates a service sharing this service's decoders while obtaining factory snapshots from a caller-owned source.
     * This advanced hook supports lifecycle-scoped adapters; ordinary standalone services should use {@link #builder()}.
     */
    public Iny withFactoryRegistry(Supplier<InyFactoryRegistry> factoryRegistrySource) {
        return new Iny(decoders, Objects.requireNonNull(factoryRegistrySource, "factoryRegistrySource"));
    }

    /** Resolves calls through the factory registry and ordinary values through decoders. */
    public <T> T resolveValue(InyValue value, Class<T> type, String path) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
        if (!(value instanceof InyCall call)) {
            return decodeValue(value, type, path);
        }
        return resolveCall(call, type, path);
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
            result = registration.factory().create(new DefaultInyFactoryContext(this, call, path));
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
    public <T> T decodeValue(InyValue value, Class<T> type, String path) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
        InyDecoder<T> decoder = decoders.find(type)
                .orElseThrow(() -> new InyMissingDecoderException(path, type));
        InyDecodeContext context = new InyDecodeContext(this, path, type, value);
        T decoded = decoder.decode(value, context);
        if (decoded == null) {
            throw context.failure("decoder returned Java null");
        }
        return decoded;
    }

    /** Builder for an immutable service and decoder registry. */
    public static final class Builder {

        private final Map<Class<?>, InyDecoder<?>> decoders = new LinkedHashMap<>();
        private final Map<InyIdentifier, InyFactoryRegistration<?>> factories = new LinkedHashMap<>();

        private Builder() {
            BuiltInDecoders.install(decoders);
        }

        /** Registers a decoder, rejecting any decoder already registered for its exact target type. */
        public <T> Builder registerDecoder(InyDecoder<T> decoder) {
            Objects.requireNonNull(decoder, "decoder");
            Class<T> targetType = Objects.requireNonNull(decoder.targetType(), "decoder targetType");
            if (decoders.putIfAbsent(targetType, decoder) != null) {
                throw new IllegalArgumentException("A decoder is already registered for " + targetType.getTypeName());
            }
            return this;
        }

        /** Explicitly replaces an existing decoder; use this when overriding is intentional. */
        public <T> Builder replaceDecoder(InyDecoder<T> decoder) {
            Objects.requireNonNull(decoder, "decoder");
            Class<T> targetType = Objects.requireNonNull(decoder.targetType(), "decoder targetType");
            if (!decoders.containsKey(targetType)) {
                throw new IllegalArgumentException("No decoder is registered for " + targetType.getTypeName());
            }
            decoders.put(targetType, decoder);
            return this;
        }

        /** Registers a concise lambda factory under a parsed namespaced identifier. */
        public <T> Builder registerFactory(
                InyIdentifier identifier,
                Class<T> resultType,
                InyFactory<T> factory
        ) {
            return registerFactory(new InyFactoryRegistration<>(identifier, resultType, factory));
        }

        /** Registers a concise lambda factory using a canonical {@code namespace:name} string. */
        public <T> Builder registerFactory(String identifier, Class<T> resultType, InyFactory<T> factory) {
            return registerFactory(InyIdentifier.parse(identifier), resultType, factory);
        }

        /** Registers an advanced immutable registration, rejecting duplicate identifiers. */
        public Builder registerFactory(InyFactoryRegistration<?> registration) {
            Objects.requireNonNull(registration, "registration");
            if (factories.putIfAbsent(registration.identifier(), registration) != null) {
                throw new InyDuplicateFactoryException(registration.identifier());
            }
            return this;
        }

        /** Explicitly replaces an existing factory registration. */
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

        /** Explicitly replaces an existing factory using a canonical identifier string. */
        public <T> Builder replaceFactory(String identifier, Class<T> resultType, InyFactory<T> factory) {
            return replaceFactory(InyIdentifier.parse(identifier), resultType, factory);
        }

        /** Installs a lightweight group of related decoders and factories. */
        public Builder install(InyModule module) {
            Objects.requireNonNull(module, "module").configure(this);
            return this;
        }

        /** Builds a service with an immutable snapshot of the configured decoders. */
        public Iny build() {
            return new Iny(new InyDecoderRegistry(decoders), new InyFactoryRegistry(factories));
        }
    }
}
