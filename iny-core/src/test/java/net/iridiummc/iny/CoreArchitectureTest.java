package net.iridiummc.iny;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoreArchitectureTest {

    @Test
    void moduleExportsOnlyTheSupportedApiPackages() throws IOException {
        Path descriptorPath = Path.of("build/classes/java/main/module-info.class");
        try (InputStream input = Files.newInputStream(descriptorPath)) {
            Set<String> exports = ModuleDescriptor.read(input).exports().stream()
                    .map(ModuleDescriptor.Exports::source)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

            assertEquals(Set.of(
                    "net.iridiummc.iny.api",
                    "net.iridiummc.iny.codec",
                    "net.iridiummc.iny.exception",
                    "net.iridiummc.iny.factory",
                    "net.iridiummc.iny.source",
                    "net.iridiummc.iny.value"
            ), exports);
            assertTrue(exports.stream().noneMatch(name -> name.contains(".internal")));
        }
    }

    @Test
    void configurationApiIsAnInterfaceBackedByAnInternalImplementation() {
        var config = net.iridiummc.iny.api.Iny.builder().build().parse("value: 1");

        assertTrue(net.iridiummc.iny.api.InyConfig.class.isInterface());
        assertTrue(net.iridiummc.iny.value.InySection.class.isAssignableFrom(
                net.iridiummc.iny.api.InyConfig.class));
        assertTrue(config.getClass().getPackageName().startsWith("net.iridiummc.iny.internal."));
        assertSame(config, config.root());
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
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.iridiummc.iny.value.InyValueType"));

        Class<?> internalValueType = Class.forName(
                "net.iridiummc.iny.internal.value.InyValueType");
        assertFalse(Modifier.isPublic(internalValueType.getModifiers()));
        assertEquals(String.class,
                net.iridiummc.iny.exception.InyDecodeException.class
                        .getMethod("actualType").getReturnType());
        assertEquals(String.class,
                net.iridiummc.iny.exception.InyPathTraversalException.class
                        .getMethod("actualType").getReturnType());
        assertEquals(String.class,
                net.iridiummc.iny.exception.InyFactoryArgumentException.class
                        .getMethod("actualType").getReturnType());
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
