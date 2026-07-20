package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.exception.InySourceLoadException;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.factory.InyFactoryRegistry;
import net.iridiummc.iny.minecraft.MinecraftInyModule;
import net.iridiummc.iny.readiness.Readiness;
import net.iridiummc.iny.readiness.ReadinessState;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyContextKeyRegistry;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.Reader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The single lifecycle-scoped INY registry owned by the active Minecraft plugin instance.
 * Providers register during startup; the registry is sealed before consumers can resolve configuration.
 */
final class MinecraftInyRegistry {

    private static final String DEFAULT_CONFIG = "config.iny";

    private final Plugin plugin;
    private final Map<InyIdentifier, OwnedFactoryRegistration> registrations;
    private final Map<InyIdentifier, OwnedContextKeyRegistration> contextKeyRegistrations;
    private final AtomicReference<InyFactoryRegistry> factorySnapshot;
    private final AtomicReference<InyContextKeyRegistry> contextKeySnapshot;
    private final Iny iny;
    private final ReadinessController readiness;
    private boolean active = true;
    private boolean factoriesSealed;

    MinecraftInyRegistry(Plugin plugin) {
        this(plugin, () -> { });
    }

    MinecraftInyRegistry(Plugin plugin, Runnable readyAction) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Iny base = Iny.builder()
                .install(new MinecraftInyModule(plugin.getServer()))
                .build();
        this.registrations = new LinkedHashMap<>();
        base.factories().registrations().forEach((identifier, registration) ->
                registrations.put(identifier, new OwnedFactoryRegistration(plugin, registration)));
        this.contextKeyRegistrations = new LinkedHashMap<>();
        base.contextKeys().entries().forEach((identifier, key) ->
                contextKeyRegistrations.put(identifier, new OwnedContextKeyRegistration(plugin, key)));
        this.factorySnapshot = new AtomicReference<>(base.factories());
        this.contextKeySnapshot = new AtomicReference<>(base.contextKeys());
        this.iny = base.withRegistries(this::activeFactorySnapshot, this::activeContextKeySnapshot);
        this.readiness = new ReadinessController(plugin, new BukkitReadinessScheduler(plugin), readyAction);
        this.readiness.openRegistration();
    }

    /** Returns the plugin instance that owns this registry and its lifecycle. */
    public Plugin plugin() {
        return plugin;
    }

    /** Returns the one server-scoped service; callers may safely retain this reference. */
    public Iny iny() {
        ensureReady();
        return iny;
    }

    /** Returns whether factory registration is sealed and configuration may be consumed. */
    public synchronized boolean isReady() {
        ensureActive();
        return readiness.isReady();
    }

    /** Returns the public startup readiness barrier. */
    public Readiness readiness() {
        ensureActive();
        return readiness;
    }

    /** Registers a late-safe callback for the successful ready transition. */
    void registerReadyCallback(Runnable callback) {
        ensureActive();
        readiness.registerReadyCallback(callback);
    }

    /** Returns the current immutable factory snapshot. */
    public InyFactoryRegistry factories() {
        return activeFactorySnapshot();
    }

    /** Returns the current immutable runtime context-key snapshot. */
    public InyContextKeyRegistry contextKeys() {
        return activeContextKeySnapshot();
    }

    /** Registers a factory owned by a plugin in this server's shared registry. */
    public synchronized <T> MinecraftInyRegistry registerFactory(
            Plugin owner,
            InyIdentifier identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        return registerFactory(owner, new InyFactoryRegistration<>(identifier, resultType, factory));
    }

    /** Registers a plugin-owned factory using its canonical {@code namespace:name} identifier. */
    public <T> MinecraftInyRegistry registerFactory(
            Plugin owner,
            String identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        return registerFactory(owner, InyIdentifier.parse(identifier), resultType, factory);
    }

    /** Registers a plugin-owned deferred-action factory. */
    public MinecraftInyRegistry registerRunnable(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyRunnable> factory
    ) {
        return registerFactory(owner, identifier, InyRunnable.class, factory);
    }

    /** Registers a plugin-owned deferred-action factory by canonical identifier. */
    public MinecraftInyRegistry registerRunnable(
            Plugin owner,
            String identifier,
            InyFactory<InyRunnable> factory
    ) {
        return registerRunnable(owner, InyIdentifier.parse(identifier), factory);
    }

    /** Registers a plugin-owned deferred-value factory. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> MinecraftInyRegistry registerProvider(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        Objects.requireNonNull(factory, "factory");
        return registerFactory(owner, identifier, (Class) InyProvider.class, (InyFactory) factory);
    }

    /** Registers a plugin-owned deferred-value factory by canonical identifier. */
    public <T> MinecraftInyRegistry registerProvider(
            Plugin owner,
            String identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        return registerProvider(owner, InyIdentifier.parse(identifier), factory);
    }

    /** Registers an advanced plugin-owned registration, rejecting duplicate identifiers. */
    public synchronized MinecraftInyRegistry registerFactory(
            Plugin owner,
            InyFactoryRegistration<?> registration
    ) {
        ensureActive();
        ensureRegistrationOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(registration, "registration");
        requireAvailableFactoryIdentifier(registration.identifier());
        OwnedFactoryRegistration ownedRegistration = new OwnedFactoryRegistration(owner, registration);
        if (registrations.putIfAbsent(registration.identifier(), ownedRegistration) != null) {
            throw new InyDuplicateFactoryException(registration.identifier());
        }
        publishSnapshot();
        return this;
    }

    /** Deliberately replaces an existing factory and transfers ownership to the supplied plugin. */
    public synchronized <T> MinecraftInyRegistry replaceFactory(
            Plugin owner,
            InyIdentifier identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        ensureActive();
        ensureRegistrationOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(identifier, "identifier");
        requireAvailableFactoryIdentifier(identifier);
        if (!registrations.containsKey(identifier)) {
            throw new IllegalArgumentException("No INY factory is registered for " + identifier);
        }
        registrations.put(identifier, new OwnedFactoryRegistration(owner,
                new InyFactoryRegistration<>(identifier, resultType, factory)));
        publishSnapshot();
        return this;
    }

    /** Deliberately replaces an existing factory using a canonical identifier string. */
    public <T> MinecraftInyRegistry replaceFactory(
            Plugin owner,
            String identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        return replaceFactory(owner, InyIdentifier.parse(identifier), resultType, factory);
    }

    /** Replaces a deferred-action factory and transfers ownership. */
    public MinecraftInyRegistry replaceRunnable(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyRunnable> factory
    ) {
        return replaceFactory(owner, identifier, InyRunnable.class, factory);
    }

    /** Replaces a deferred-action factory by canonical identifier and transfers ownership. */
    public MinecraftInyRegistry replaceRunnable(
            Plugin owner,
            String identifier,
            InyFactory<InyRunnable> factory
    ) {
        return replaceRunnable(owner, InyIdentifier.parse(identifier), factory);
    }

    /** Replaces a deferred-value factory and transfers ownership. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> MinecraftInyRegistry replaceProvider(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        Objects.requireNonNull(factory, "factory");
        return replaceFactory(owner, identifier, (Class) InyProvider.class, (InyFactory) factory);
    }

    /** Replaces a deferred-value factory by canonical identifier and transfers ownership. */
    public <T> MinecraftInyRegistry replaceProvider(
            Plugin owner,
            String identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        return replaceProvider(owner, InyIdentifier.parse(identifier), factory);
    }

    /** Registers a typed runtime context key owned by a plugin. */
    public synchronized MinecraftInyRegistry registerContextKey(Plugin owner, InyContextKey<?> key) {
        ensureActive();
        ensureRegistrationOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(key, "key");
        OwnedContextKeyRegistration existing = contextKeyRegistrations.get(key.identifier());
        if (existing != null) {
            if (!existing.key().type().equals(key.type())) {
                throw incompatibleContextKey(existing.key(), key);
            }
            throw new IllegalArgumentException("A runtime context key is already registered for "
                    + key.identifier());
        }
        contextKeyRegistrations.put(key.identifier(), new OwnedContextKeyRegistration(owner, key));
        publishContextKeySnapshot();
        return this;
    }

    /** Replaces a typed runtime context key and transfers ownership without changing its type. */
    public synchronized MinecraftInyRegistry replaceContextKey(Plugin owner, InyContextKey<?> key) {
        ensureActive();
        ensureRegistrationOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(key, "key");
        OwnedContextKeyRegistration existing = contextKeyRegistrations.get(key.identifier());
        if (existing == null) {
            throw new IllegalArgumentException("No runtime context key is registered for " + key.identifier());
        }
        if (!existing.key().type().equals(key.type())) {
            throw incompatibleContextKey(existing.key(), key);
        }
        contextKeyRegistrations.put(key.identifier(), new OwnedContextKeyRegistration(owner, key));
        publishContextKeySnapshot();
        return this;
    }

    /** Parses through the shared server service. */
    public InyConfig parse(String source) {
        return iny().parse(source);
    }

    /** Parses a named source through the shared server service. */
    public InyConfig parse(String sourceName, String source) {
        return iny().parse(sourceName, source);
    }

    /** Parses a caller-owned reader through the shared server service. */
    public InyConfig parse(String sourceName, Reader reader) {
        return iny().parse(sourceName, reader);
    }

    /** Loads a UTF-8 configuration through the shared server service. */
    public InyConfig load(Path path) {
        return iny().load(path);
    }

    /** Loads the conventional INY configuration belonging to another plugin. */
    public InyConfig loadConfig(Plugin plugin) {
        return loadConfig(plugin, DEFAULT_CONFIG);
    }

    /** Loads a plugin-relative INY configuration and installs its bundled default when absent. */
    public InyConfig loadConfig(Plugin plugin, String resourcePath) {
        ensureReady();
        Objects.requireNonNull(plugin, "plugin");
        Path relativePath = validateResourcePath(resourcePath);
        File dataFolder = Objects.requireNonNull(plugin.getDataFolder(), "plugin data folder");
        Path configPath = dataFolder.toPath().toAbsolutePath().normalize().resolve(relativePath).normalize();

        if (!configPath.startsWith(dataFolder.toPath().toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("INY config resource must remain inside the plugin data folder: "
                    + resourcePath);
        }
        if (!java.nio.file.Files.exists(configPath)) {
            try {
                String normalizedResourcePath = relativePath.toString().replace(File.separatorChar, '/');
                plugin.saveResource(normalizedResourcePath, false);
            } catch (RuntimeException exception) {
                throw new InySourceLoadException(configPath, exception);
            }
        }
        return load(configPath);
    }

    synchronized void close() {
        readiness.shutdown();
        active = false;
    }

    /** Seals factory and blocker registration at the startup barrier. */
    synchronized boolean sealFactories() {
        ensureActive();
        if (factoriesSealed) {
            return false;
        }
        factoriesSealed = true;
        readiness.closeRegistration();
        return true;
    }

    /** Removes factories and resolves readiness blockers owned by a disabled plugin. */
    synchronized void handlePluginDisable(Plugin disabledPlugin) {
        Objects.requireNonNull(disabledPlugin, "disabledPlugin");
        if (!active) {
            return;
        }
        boolean removed = registrations.entrySet().removeIf(entry ->
                entry.getValue().owner() == disabledPlugin);
        boolean contextKeysRemoved = contextKeyRegistrations.entrySet().removeIf(entry ->
                entry.getValue().owner() == disabledPlugin);
        if (removed) {
            publishSnapshot();
        }
        if (contextKeysRemoved) {
            publishContextKeySnapshot();
        }
        readiness.handlePluginDisable(disabledPlugin);
    }

    private void publishSnapshot() {
        Map<InyIdentifier, InyFactoryRegistration<?>> snapshot = new LinkedHashMap<>();
        registrations.forEach((identifier, ownedRegistration) ->
                snapshot.put(identifier, ownedRegistration.registration()));
        factorySnapshot.set(new InyFactoryRegistry(snapshot));
    }

    private void publishContextKeySnapshot() {
        Map<InyIdentifier, InyContextKey<?>> snapshot = new LinkedHashMap<>();
        contextKeyRegistrations.forEach((identifier, registration) ->
                snapshot.put(identifier, registration.key()));
        contextKeySnapshot.set(InyContextKeyRegistry.copyOf(snapshot));
    }

    private InyFactoryRegistry activeFactorySnapshot() {
        ensureReady();
        return factorySnapshot.get();
    }

    private InyContextKeyRegistry activeContextKeySnapshot() {
        ensureReady();
        return contextKeySnapshot.get();
    }

    private synchronized void ensureActive() {
        if (!active) {
            throw new IllegalStateException("The owning INY plugin instance is no longer active");
        }
    }

    private synchronized void ensureReady() {
        ensureActive();
        if (!readiness.isReady()) {
            throw new IllegalStateException(
                    "INY is not ready (state: " + readiness.state()
                            + "); wait for InyReadyEvent before loading configuration");
        }
    }

    private void ensureRegistrationOpen() {
        if (factoriesSealed || readiness.state() == ReadinessState.FAILED) {
            throw new IllegalStateException(
                    "INY factory registration is closed; factories must be registered during the startup registration phase");
        }
    }

    private static void requireAvailableFactoryIdentifier(InyIdentifier identifier) {
        if (identifier.namespace().equals("context") && !identifier.value().equals("value")) {
            throw new IllegalArgumentException("The 'context' namespace is reserved for runtime context access");
        }
    }

    private static IllegalArgumentException incompatibleContextKey(
            InyContextKey<?> existing,
            InyContextKey<?> replacement
    ) {
        return new IllegalArgumentException("Context key '" + replacement.identifier()
                + "' is already registered as " + existing.type().getTypeName()
                + " and cannot also use " + replacement.type().getTypeName());
    }

    private static Path validateResourcePath(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        if (resourcePath.isBlank()) {
            throw new IllegalArgumentException("INY config resource path cannot be blank");
        }
        Path path;
        try {
            path = Path.of(resourcePath).normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid INY config resource path: " + resourcePath, exception);
        }
        if (path.isAbsolute() || path.getNameCount() == 0 || path.startsWith("..")) {
            throw new IllegalArgumentException("INY config resource must be a plugin-relative path: "
                    + resourcePath);
        }
        return path;
    }

    private record OwnedFactoryRegistration(
            Plugin owner,
            InyFactoryRegistration<?> registration
    ) {
        private OwnedFactoryRegistration {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(registration, "registration");
        }
    }

    private record OwnedContextKeyRegistration(Plugin owner, InyContextKey<?> key) {
        private OwnedContextKeyRegistration {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(key, "key");
        }
    }
}
