package net.iridiummc.iny;

import net.iridiummc.iny.api.InyConfig;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftGlobalRegistryTest {

    @Test
    void activePluginOwnsOneSharedRegistry() {
        Plugin plugin = plugin();
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin);

        assertFalse(registry.isReady());
        registry.sealFactories();

        assertSame(plugin, registry.plugin());
        assertSame(registry.iny(), registry.iny());
        assertTrue(registry.isReady());
        assertEquals(6, registry.factories().size());
    }

    @Test
    void dependentPluginsCanRegisterFactoriesInTheSharedRegistry() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin());

        registry.registerFactory("external:named", CustomNamedValue.class, context -> {
            context.arguments().requireCount(1);
            return new CustomNamedValue(context.arguments().get(0, String.class));
        });
        registry.sealFactories();

        NamedValue value = registry.parse("value: external:named(\"shared\")\n")
                .get("value", NamedValue.class);
        assertEquals("shared", value.name());
        assertTrue(registry.factories().contains(
                net.iridiummc.iny.api.InyIdentifier.parse("external:named")));
    }

    @Test
    void independentProvidersCompleteRegistrationBeforeTheReadyBarrier() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin());
        registry.registerFactory("first:available", CustomNamedValue.class,
                context -> new CustomNamedValue("first"));
        registry.registerFactory("second:available", CustomNamedValue.class,
                context -> new CustomNamedValue("second"));
        registry.sealFactories();

        InyConfig config = registry.parse("""
                first: first:available()
                second: second:available()
                """);

        assertEquals("first", config.get("first", NamedValue.class).name());
        assertEquals("second", config.get("second", NamedValue.class).name());
    }

    @Test
    void duplicatesRemainRejectedAndSnapshotsRemainImmutable() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin());
        registry.registerFactory("external:value", String.class, context -> "one");

        assertThrows(InyDuplicateFactoryException.class,
                () -> registry.registerFactory("external:value", String.class, context -> "two"));
        registry.sealFactories();
        assertThrows(UnsupportedOperationException.class,
                () -> registry.factories().registrations().clear());
    }

    @Test
    void readinessBarrierRejectsEarlyConsumptionAndLateRegistration() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin());

        assertThrows(IllegalStateException.class, registry::iny);
        assertThrows(IllegalStateException.class, registry::factories);
        assertThrows(IllegalStateException.class, () -> registry.parse("value: ready\n"));
        assertThrows(IllegalStateException.class, () -> registry.loadConfig(registry.plugin()));

        registry.registerFactory("external:value", String.class, context -> "ready");
        registry.sealFactories();

        assertEquals("ready", registry.parse("value: external:value()\n")
                .get("value", String.class));
        assertThrows(IllegalStateException.class,
                () -> registry.registerFactory("external:late", String.class, context -> "late"));
        assertThrows(IllegalStateException.class,
                () -> registry.replaceFactory("external:value", String.class, context -> "late"));
    }

    @Test
    void closingThePluginRegistryInvalidatesCachedServices() {
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin());
        registry.sealFactories();
        InyConfig cachedConfig = registry.parse("value: minecraft:material(\"diamond\")\n");

        registry.close();

        assertThrows(IllegalStateException.class, registry::iny);
        assertThrows(IllegalStateException.class,
                () -> cachedConfig.get("value", org.bukkit.Material.class));
        assertThrows(IllegalStateException.class,
                () -> registry.registerFactory("external:value", String.class, context -> "late"));
    }

    @Test
    void loadsTheConventionalConfigFromTheDependentPluginsDataFolder(@TempDir Path dataFolder)
            throws IOException {
        Files.writeString(dataFolder.resolve("config.iny"), "name: existing\n");
        MinecraftInyRegistry registry = new MinecraftInyRegistry(plugin(dataFolder, Map.of()));
        registry.sealFactories();

        InyConfig config = registry.loadConfig(registry.plugin());

        assertEquals("existing", config.get("name", String.class));
    }

    @Test
    void installsABundledPluginConfigOnFirstLoad(@TempDir Path dataFolder) {
        Plugin dependentPlugin = plugin(dataFolder, Map.of(
                "config.iny", "name: bundled\n",
                "presets/rewards.iny", "amount: 12\n"));
        MinecraftInyRegistry registry = new MinecraftInyRegistry(dependentPlugin);
        registry.sealFactories();

        assertEquals("bundled", registry.loadConfig(dependentPlugin).get("name", String.class));
        assertEquals(12, registry.loadConfig(dependentPlugin, "presets/rewards.iny")
                .get("amount", Integer.class));
        assertTrue(Files.isRegularFile(dataFolder.resolve("config.iny")));
        assertTrue(Files.isRegularFile(dataFolder.resolve("presets/rewards.iny")));
    }

    @Test
    void rejectsConfigPathsOutsideTheDependentPluginsDataFolder(@TempDir Path dataFolder) {
        Plugin dependentPlugin = plugin(dataFolder, Map.of());
        MinecraftInyRegistry registry = new MinecraftInyRegistry(dependentPlugin);
        registry.sealFactories();

        assertThrows(IllegalArgumentException.class,
                () -> registry.loadConfig(dependentPlugin, "../outside.iny"));
        assertThrows(IllegalArgumentException.class,
                () -> registry.loadConfig(dependentPlugin, dataFolder.resolve("absolute.iny").toString()));
    }

    private static Plugin plugin() {
        return plugin(Path.of("build", "test-plugin-data"), Map.of());
    }

    private static Plugin plugin(Path dataFolder, Map<String, String> resources) {
        Server server = proxy(Server.class, (method, arguments) -> defaultValue(method.getReturnType()));
        return proxy(Plugin.class, (method, arguments) -> {
            return switch (method.getName()) {
                case "getServer" -> server;
                case "getDataFolder" -> dataFolder.toFile();
                case "saveResource" -> {
                    String resourcePath = (String) arguments[0];
                    String contents = resources.get(resourcePath);
                    if (contents == null) {
                        throw new IllegalArgumentException("No bundled resource " + resourcePath);
                    }
                    Path destination = dataFolder.resolve(resourcePath);
                    try {
                        Files.createDirectories(destination.getParent());
                        Files.writeString(destination, contents);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            default -> null;
                        };
                    }
                    return invocation.invoke(method, arguments);
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments);
    }

    private interface NamedValue {
        String name();
    }

    private record CustomNamedValue(String name) implements NamedValue { }
}
