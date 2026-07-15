package net.iridiummc.iny.internal.source;

import net.iridiummc.iny.exception.InySourceLoadException;
import net.iridiummc.iny.source.InySource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Internal source acquisition, deliberately separate from lexing and parsing. */
public final class SourceLoading {

    private SourceLoading() {
    }

    public static InySource read(String sourceName, Reader reader) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(reader, "reader");
        try {
            StringBuilder source = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (read > 0) {
                    source.append(buffer, 0, read);
                }
            }
            return new InySource(sourceName, source.toString());
        } catch (IOException exception) {
            throw new InySourceLoadException(sourceName, exception);
        }
    }

    public static InySource load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return new InySource(path.toString(), Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new InySourceLoadException(path, exception);
        }
    }
}
