package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.minecraft.MinecraftInyModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftInyModuleTest {

    @Test
    void minecraftFactoriesUseTheSamePublicRegistry() {
        Fixtures fixtures = fixtures();
        Iny iny = Iny.builder().install(new MinecraftInyModule(fixtures.server())).build();

        assertEquals(7, iny.factories().size());
        assertTrue(iny.factories().contains(InyIdentifier.parse("minecraft:location")));
        assertTrue(iny.factories().contains(InyIdentifier.parse("minecraft:world")));
        assertTrue(iny.factories().contains(InyIdentifier.parse("minecraft:material")));
        assertTrue(iny.factories().contains(InyIdentifier.parse("minecraft:vector")));
        assertTrue(iny.factories().contains(InyIdentifier.parse("minecraft:block")));
        assertTrue(iny.factories().contains(InyIdentifier.parse("context:value")));
        assertEquals(MinecraftContextKeys.PLAYER,
                iny.contextKeys().require(InyIdentifier.parse("minecraft:player")));
    }

    @Test
    void constructsLocationWithOptionalRotationAndWorld() {
        Fixtures fixtures = fixtures();
        var config = service(fixtures).parse("""
                spawn: minecraft:location("world", 10, 64, -20)
                rotated: minecraft:location("world", 1.5, 2.5, 3.5, 90, -15)
                world: minecraft:world("world")
                """);

        Location spawn = config.get("spawn", Location.class);
        assertSame(fixtures.world(), spawn.getWorld());
        assertEquals(10, spawn.getX());
        assertEquals(64, spawn.getY());
        assertEquals(-20, spawn.getZ());
        assertEquals(0, spawn.getYaw());
        assertEquals(0, spawn.getPitch());

        Location rotated = config.get("rotated", Location.class);
        assertEquals(90, rotated.getYaw());
        assertEquals(-15, rotated.getPitch());
        assertSame(fixtures.world(), config.get("world", World.class));
    }

    @Test
    void constructsMaterialVectorAndIntegralBlock() {
        Fixtures fixtures = fixtures();
        var config = service(fixtures).parse("""
                material: minecraft:material("diamond")
                vector: minecraft:vector(1, 0.5, -1)
                block: minecraft:block("world", 10, 64, -20)
                bad_block: minecraft:block("world", 10.5, 64, -20)
                """);

        assertEquals(Material.DIAMOND, config.get("material", Material.class));
        assertEquals(new Vector(1, 0.5, -1), config.get("vector", Vector.class));
        assertSame(fixtures.block(), config.get("block", Block.class));
        assertEquals("10,64,-20", fixtures.blockCoordinates().get());
        assertThrows(RuntimeException.class, () -> config.get("bad_block", Block.class));
    }

    private static Iny service(Fixtures fixtures) {
        return Iny.builder().install(new MinecraftInyModule(fixtures.server())).build();
    }

    private static Fixtures fixtures() {
        AtomicReference<String> coordinates = new AtomicReference<>();
        Block block = proxy(Block.class, (method, arguments) -> defaultValue(method.getReturnType()));
        World world = proxy(World.class, (method, arguments) -> {
            if (method.getName().equals("getBlockAt") && arguments != null && arguments.length == 3) {
                coordinates.set(arguments[0] + "," + arguments[1] + "," + arguments[2]);
                return block;
            }
            return defaultValue(method.getReturnType());
        });
        Server server = proxy(Server.class, (method, arguments) -> {
            if (method.getName().equals("getWorld") && arguments != null
                    && arguments.length == 1 && arguments[0].equals("world")) {
                return world;
            }
            return defaultValue(method.getReturnType());
        });
        return new Fixtures(server, world, block, coordinates);
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

    private record Fixtures(Server server, World world, Block block, AtomicReference<String> blockCoordinates) { }
}
