package net.iridiummc.iny;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CoreArchitectureTest {

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
