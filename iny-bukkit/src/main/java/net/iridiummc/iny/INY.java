package net.iridiummc.iny;

import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.factory.InyFactoryRegistry;
import net.iridiummc.iny.readiness.Readiness;
import net.iridiummc.iny.runtime.InyContextKey;
import net.iridiummc.iny.runtime.InyContextKeyRegistry;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bukkit entry point and public facade for the one server-wide INY service.
 * Plugins using INY must depend on this plugin and obtain this active instance through
 * {@link #getInstance()} rather than constructing a separate service.
 */
public final class INY extends JavaPlugin implements Listener {

    private static volatile INY instance;
    private MinecraftInyRegistry ownedRegistry;
    private volatile InyReadyEvent readyEvent;

    /** Creates the Bukkit-managed plugin entry point. */
    public INY() {
    }

    /**
     * Returns the active INY plugin instance.
     *
     * @return the active plugin instance
     */
    public static INY getInstance() {
        INY current = instance;
        if (current == null) {
            throw new IllegalStateException("The INY plugin is not loaded");
        }
        return current;
    }

    /**
     * Returns whether startup registration has ended and configuration may be consumed.
     *
     * @return {@code true} when the shared service is ready
     */
    public boolean isReady() {
        return registry().isReady();
    }

    /**
     * Returns the startup readiness barrier used by external integrations.
     *
     * @return the shared readiness barrier
     */
    public Readiness readiness() {
        return registry().readiness();
    }

    /**
     * Registers a callback for successful readiness. Late registrations are scheduled on Bukkit's main thread.
     * Bukkit {@link InyReadyEvent} listeners remain supported and are fired first.
     *
     * @param callback callback to run after the ready event
     */
    public void onInyReady(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        registry().registerReadyCallback(callback);
    }

    /**
     * Registers a late-safe callback that receives the same event type used by Bukkit listeners.
     *
     * @param callback callback to run after the ready event
     */
    public void onInyReady(Consumer<InyReadyEvent> callback) {
        Objects.requireNonNull(callback, "callback");
        registry().registerReadyCallback(() -> {
            InyReadyEvent event = readyEvent;
            if (event != null) {
                callback.accept(event);
            }
        });
    }

    /**
     * Loads {@code config.iny} from a dependent plugin's data folder.
     * If it does not exist yet, the plugin's bundled {@code config.iny} resource is copied first.
     *
     * @param plugin plugin whose configuration should be loaded
     * @return the parsed immutable configuration
     */
    public InyConfig loadConfig(Plugin plugin) {
        return registry().loadConfig(plugin);
    }

    /**
     * Loads a plugin-relative INY file, copying the same bundled resource on first use.
     * The resource path must remain inside the dependent plugin's data folder.
     *
     * @param plugin plugin whose configuration should be loaded
     * @param resourcePath plugin-relative resource and destination path
     * @return the parsed immutable configuration
     */
    public InyConfig loadConfig(Plugin plugin, String resourcePath) {
        return registry().loadConfig(plugin, resourcePath);
    }

    /**
     * Loads an existing UTF-8 INY file after {@link InyReadyEvent} through the shared service.
     *
     * @param path file to load
     * @return the parsed immutable configuration
     */
    public InyConfig load(Path path) {
        return registry().load(path);
    }

    /**
     * Registers a factory owned by the providing plugin in the shared server registry.
     * Factory-providing plugins should call this synchronously from {@link Plugin#onEnable()}.
     *
     * @param owner plugin that owns the registration
     * @param identifier namespaced factory identifier
     * @param resultType declared factory result type
     * @param factory factory implementation
     * @param <T> factory result type
     * @return this plugin facade
     */
    public <T> INY registerFactory(
            Plugin owner,
            InyIdentifier identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().registerFactory(owner, identifier, resultType, factory);
        return this;
    }

    /**
     * Registers a factory under a canonical {@code namespace:name} identifier.
     * Factory-providing plugins should call this synchronously from {@link Plugin#onEnable()}.
     *
     * @param owner plugin that owns the registration
     * @param identifier canonical factory identifier
     * @param resultType declared factory result type
     * @param factory factory implementation
     * @param <T> factory result type
     * @return this plugin facade
     */
    public <T> INY registerFactory(
            Plugin owner,
            String identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().registerFactory(owner, identifier, resultType, factory);
        return this;
    }

    /**
     * Registers an advanced immutable factory registration.
     * Factory-providing plugins should call this synchronously from {@link Plugin#onEnable()}.
     *
     * @param owner plugin that owns the registration
     * @param registration registration to add
     * @return this plugin facade
     */
    public INY registerFactory(Plugin owner, InyFactoryRegistration<?> registration) {
        registry().registerFactory(owner, registration);
        return this;
    }

    /**
     * Registers a deferred action owned by the providing plugin.
     *
     * @param owner plugin owning the registration
     * @param identifier namespaced factory identifier
     * @param factory deferred-action factory
     * @return this plugin facade
     */
    public INY registerRunnable(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyRunnable> factory
    ) {
        registry().registerRunnable(owner, identifier, factory);
        return this;
    }

    /**
     * Registers a deferred action under a canonical identifier.
     *
     * @param owner plugin owning the registration
     * @param identifier canonical factory identifier
     * @param factory deferred-action factory
     * @return this plugin facade
     */
    public INY registerRunnable(Plugin owner, String identifier, InyFactory<InyRunnable> factory) {
        registry().registerRunnable(owner, identifier, factory);
        return this;
    }

    /**
     * Registers a deferred value provider owned by the providing plugin.
     *
     * @param owner plugin owning the registration
     * @param identifier namespaced factory identifier
     * @param factory deferred-value factory
     * @param <T> provider result type
     * @return this plugin facade
     */
    public <T> INY registerProvider(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        registry().registerProvider(owner, identifier, factory);
        return this;
    }

    /**
     * Registers a deferred value provider under a canonical identifier.
     *
     * @param owner plugin owning the registration
     * @param identifier canonical factory identifier
     * @param factory deferred-value factory
     * @param <T> provider result type
     * @return this plugin facade
     */
    public <T> INY registerProvider(
            Plugin owner,
            String identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        registry().registerProvider(owner, identifier, factory);
        return this;
    }

    /**
     * Registers a typed runtime context key owned by the providing plugin.
     *
     * @param owner plugin owning the registration
     * @param key context key to register
     * @return this plugin facade
     */
    public INY registerContextKey(Plugin owner, InyContextKey<?> key) {
        registry().registerContextKey(owner, key);
        return this;
    }

    /**
     * Replaces a factory and transfers its ownership while startup registration remains open.
     *
     * @param owner plugin that will own the replacement
     * @param identifier identifier of the registration to replace
     * @param resultType declared replacement result type
     * @param factory replacement factory
     * @param <T> factory result type
     * @return this plugin facade
     */
    public <T> INY replaceFactory(
            Plugin owner,
            InyIdentifier identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().replaceFactory(owner, identifier, resultType, factory);
        return this;
    }

    /**
     * Replaces a deferred action and transfers ownership to the providing plugin.
     *
     * @param owner plugin that will own the replacement
     * @param identifier registered factory identifier
     * @param factory replacement deferred-action factory
     * @return this plugin facade
     */
    public INY replaceRunnable(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyRunnable> factory
    ) {
        registry().replaceRunnable(owner, identifier, factory);
        return this;
    }

    /**
     * Replaces a deferred action by canonical identifier and transfers ownership.
     *
     * @param owner plugin that will own the replacement
     * @param identifier canonical registered identifier
     * @param factory replacement deferred-action factory
     * @return this plugin facade
     */
    public INY replaceRunnable(Plugin owner, String identifier, InyFactory<InyRunnable> factory) {
        registry().replaceRunnable(owner, identifier, factory);
        return this;
    }

    /**
     * Replaces a deferred value provider and transfers ownership.
     *
     * @param owner plugin that will own the replacement
     * @param identifier registered factory identifier
     * @param factory replacement deferred-value factory
     * @param <T> provider result type
     * @return this plugin facade
     */
    public <T> INY replaceProvider(
            Plugin owner,
            InyIdentifier identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        registry().replaceProvider(owner, identifier, factory);
        return this;
    }

    /**
     * Replaces a deferred value provider by canonical identifier and transfers ownership.
     *
     * @param owner plugin that will own the replacement
     * @param identifier canonical registered identifier
     * @param factory replacement deferred-value factory
     * @param <T> provider result type
     * @return this plugin facade
     */
    public <T> INY replaceProvider(
            Plugin owner,
            String identifier,
            InyFactory<InyProvider<T>> factory
    ) {
        registry().replaceProvider(owner, identifier, factory);
        return this;
    }

    /**
     * Replaces a typed runtime context key and transfers ownership without changing its type.
     *
     * @param owner plugin that will own the replacement
     * @param key replacement context key
     * @return this plugin facade
     */
    public INY replaceContextKey(Plugin owner, InyContextKey<?> key) {
        registry().replaceContextKey(owner, key);
        return this;
    }

    /**
     * Replaces a factory by canonical identifier and transfers ownership while registration remains open.
     *
     * @param owner plugin that will own the replacement
     * @param identifier canonical identifier of the registration to replace
     * @param resultType declared replacement result type
     * @param factory replacement factory
     * @param <T> factory result type
     * @return this plugin facade
     */
    public <T> INY replaceFactory(
            Plugin owner,
            String identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().replaceFactory(owner, identifier, resultType, factory);
        return this;
    }

    /**
     * Returns the sealed immutable factory snapshot after {@link InyReadyEvent}.
     *
     * @return the shared factory registry snapshot
     */
    public InyFactoryRegistry factories() {
        return registry().factories();
    }

    /**
     * Returns the sealed immutable runtime context-key snapshot after readiness.
     *
     * @return current context-key registry snapshot
     */
    public InyContextKeyRegistry contextKeys() {
        return registry().contextKeys();
    }

    @Override
    public void onLoad() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(this, this::publishReadyEvent);
        activate(this, registry);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Ends registration after server startup and announces that configuration consumption is safe.
     *
     * @param event Bukkit server-load event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        registry().sealFactories();
    }

    /**
     * Removes factories and resolves blockers owned by plugins that Bukkit disables.
     *
     * @param event Bukkit plugin-disable event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        MinecraftInyRegistry registry = ownedRegistry;
        if (registry != null) {
            registry.handlePluginDisable(event.getPlugin());
        }
    }

    @Override
    public void onDisable() {
        deactivate(this);
    }

    private static synchronized void activate(INY plugin, MinecraftInyRegistry registry) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
        if (instance != null) {
            throw new IllegalStateException("An INY plugin instance is already active");
        }
        plugin.ownedRegistry = registry;
        instance = plugin;
    }

    private static synchronized void deactivate(INY plugin) {
        if (instance == plugin) {
            MinecraftInyRegistry registry = plugin.ownedRegistry;
            plugin.ownedRegistry = null;
            instance = null;
            if (registry != null) {
                registry.close();
            }
        }
    }

    private MinecraftInyRegistry registry() {
        MinecraftInyRegistry registry = ownedRegistry;
        if (registry == null || instance != this) {
            throw new IllegalStateException("This INY plugin instance is not active");
        }
        return registry;
    }

    private void publishReadyEvent() {
        InyReadyEvent event = new InyReadyEvent(this);
        readyEvent = event;
        getServer().getPluginManager().callEvent(event);
    }
}
