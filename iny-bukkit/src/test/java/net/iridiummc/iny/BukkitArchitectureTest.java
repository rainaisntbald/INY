package net.iridiummc.iny;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitArchitectureTest {

    @Test
    void bundledModuleExportsOnlyTheSupportedApiPackages() throws IOException {
        Path descriptorPath = Path.of("build/classes/java/main/module-info.class");
        try (InputStream input = Files.newInputStream(descriptorPath)) {
            Set<String> exports = ModuleDescriptor.read(input).exports().stream()
                    .map(ModuleDescriptor.Exports::source)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

            assertEquals(Set.of(
                    "net.iridiummc.iny",
                    "net.iridiummc.iny.api",
                    "net.iridiummc.iny.codec",
                    "net.iridiummc.iny.exception",
                    "net.iridiummc.iny.factory",
                    "net.iridiummc.iny.readiness",
                    "net.iridiummc.iny.source",
                    "net.iridiummc.iny.value"
            ), exports);
            assertTrue(exports.stream().noneMatch(name -> name.contains(".internal")));
        }
    }
}
