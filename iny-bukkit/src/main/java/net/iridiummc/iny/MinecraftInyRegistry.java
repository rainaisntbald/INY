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
    private final AtomicReference<InyFactoryRegistry> factorySnapshot;
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
        this.factorySnapshot = new AtomicReference<>(base.factories());
        this.iny = base.withFactoryRegistry(this::activeFactorySnapshot);
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

    /** Registers an advanced plugin-owned registration, rejecting duplicate identifiers. */
    public synchronized MinecraftInyRegistry registerFactory(
            Plugin owner,
            InyFactoryRegistration<?> registration
    ) {
        ensureActive();
        ensureRegistrationOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(registration, "registration");
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
        if (removed) {
            publishSnapshot();
        }
        readiness.handlePluginDisable(disabledPlugin);
    }

    private void publishSnapshot() {
        Map<InyIdentifier, InyFactoryRegistration<?>> snapshot = new LinkedHashMap<>();
        registrations.forEach((identifier, ownedRegistration) ->
                snapshot.put(identifier, ownedRegistration.registration()));
        factorySnapshot.set(new InyFactoryRegistry(snapshot));
    }

    private InyFactoryRegistry activeFactorySnapshot() {
        ensureReady();
        return factorySnapshot.get();
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
}
