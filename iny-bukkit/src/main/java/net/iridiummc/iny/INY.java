package net.iridiummc.iny;

import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.factory.InyFactoryRegistry;
import net.iridiummc.iny.readiness.Readiness;
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

    /** Returns the active INY plugin instance. */
    public static INY getInstance() {
        INY current = instance;
        if (current == null) {
            throw new IllegalStateException("The INY plugin is not loaded");
        }
        return current;
    }

    /** Returns whether startup registration has ended and configuration may be consumed. */
    public boolean isReady() {
        return registry().isReady();
    }

    /** Returns the startup readiness barrier used by external integrations. */
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
     */
    public InyConfig loadConfig(Plugin plugin) {
        return registry().loadConfig(plugin);
    }

    /**
     * Loads a plugin-relative INY file, copying the same bundled resource on first use.
     * The resource path must remain inside the dependent plugin's data folder.
     */
    public InyConfig loadConfig(Plugin plugin, String resourcePath) {
        return registry().loadConfig(plugin, resourcePath);
    }

    /** Loads an existing UTF-8 INY file after {@link InyReadyEvent} through the shared service. */
    public InyConfig load(Path path) {
        return registry().load(path);
    }

    /**
     * Registers a factory owned by the providing plugin in the shared server registry.
     * Factory-providing plugins should call this synchronously from {@link Plugin#onEnable()}.
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
     */
    public INY registerFactory(Plugin owner, InyFactoryRegistration<?> registration) {
        registry().registerFactory(owner, registration);
        return this;
    }

    /** Replaces a factory and transfers its ownership while startup registration remains open. */
    public <T> INY replaceFactory(
            Plugin owner,
            InyIdentifier identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().replaceFactory(owner, identifier, resultType, factory);
        return this;
    }

    /** Replaces a factory by canonical identifier and transfers ownership while registration remains open. */
    public <T> INY replaceFactory(
            Plugin owner,
            String identifier,
            Class<T> resultType,
            InyFactory<T> factory
    ) {
        registry().replaceFactory(owner, identifier, resultType, factory);
        return this;
    }

    /** Returns the sealed immutable factory snapshot after {@link InyReadyEvent}. */
    public InyFactoryRegistry factories() {
        return registry().factories();
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

    /** Ends registration after server startup and announces that configuration consumption is safe. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        registry().sealFactories();
    }

    /** Removes factories and resolves blockers owned by plugins that Bukkit disables. */
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
