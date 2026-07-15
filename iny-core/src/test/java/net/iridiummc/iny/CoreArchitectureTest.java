package net.iridiummc.iny;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreArchitectureTest {

    @Test
    void configurationApiIsAnInterfaceBackedByAnInternalImplementation() {
        var config = net.iridiummc.iny.api.Iny.builder().build().parse("value: 1");

        assertTrue(net.iridiummc.iny.api.InyConfig.class.isInterface());
        assertTrue(config.getClass().getPackageName().startsWith("net.iridiummc.iny.internal."));
    }

    @Test
    void publicApiExposesJavaValuesAndOnlyTheStructuralSectionType() throws Exception {
        Class<?> config = net.iridiummc.iny.api.InyConfig.class;

        assertTrue(net.iridiummc.iny.value.InySection.class.isInterface());
        assertTrue(net.iridiummc.iny.codec.InyDecodeContext.class.isInterface());
        assertEquals(Object.class, config.getMethod("getValue", String.class).getReturnType());
        assertEquals(List.class, config.getMethod("getList", String.class).getReturnType());
        assertEquals(Object.class,
                net.iridiummc.iny.factory.InyArguments.class.getMethod("value", int.class).getReturnType());

        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.iridiummc.iny.value.InyValue"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.iridiummc.iny.value.InyList"));
        assertThrows(NoSuchMethodException.class,
                () -> net.iridiummc.iny.api.Iny.class.getMethod(
                        "resolveValue", Object.class, Class.class, String.class));
        assertThrows(NoSuchMethodException.class,
                () -> net.iridiummc.iny.api.Iny.class.getMethod(
                        "decodeValue", Object.class, Class.class, String.class));
    }

    @Test
    void coreSourcesHaveNoBukkitDependency() throws IOException {
        Path sourceDirectory = Path.of("src/main/java");
        try (var paths = Files.walk(sourceDirectory)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("org.bukkit"), () -> path + " imports Bukkit");
            }
        }
    }
}
