package net.iridiummc.iny;

import net.iridiummc.iny.api.Iny;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationExamplesTest {

    private static final Pattern INY_BLOCK = Pattern.compile(
            "(?ms)^```iny\\R(.*?)^```[ \\t]*$");

    @Test
    void documentedInyExamplesParse() throws IOException {
        Iny iny = Iny.builder().build();
        for (Path document : List.of(
                Path.of("../docs/bukkit.md"),
                Path.of("../docs/core.md"),
                Path.of("../docs/iny.md")
        )) {
            var blocks = INY_BLOCK.matcher(Files.readString(document));
            int index = 0;
            while (blocks.find()) {
                iny.parse(document.getFileName() + " example " + ++index, blocks.group(1));
            }
            assertTrue(index > 0, () -> document + " contains no INY examples");
        }
    }
}
