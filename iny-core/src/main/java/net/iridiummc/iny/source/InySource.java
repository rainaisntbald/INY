package net.iridiummc.iny.source;

import java.util.Objects;

/**
 * Source text together with the name used in diagnostics.
 *
 * @param name diagnostic source name
 * @param text complete source text
 */
public record InySource(String name, String text) {

    /** Validates and creates a named source. */
    public InySource {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(text, "text");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Source name must not be blank");
        }
    }

    /**
     * Returns the source line using a one-based line number.
     *
     * @param lineNumber one-based line number
     * @return the requested line, or an empty string when it does not exist
     */
    public String line(int lineNumber) {
        if (lineNumber < 1) {
            return "";
        }

        int currentLine = 1;
        int start = 0;
        for (int index = 0; index <= text.length(); index++) {
            boolean atEnd = index == text.length();
            char character = atEnd ? '\0' : text.charAt(index);
            if (atEnd || character == '\n' || character == '\r') {
                if (currentLine == lineNumber) {
                    return text.substring(start, index);
                }
                if (character == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                currentLine++;
                start = index + 1;
            }
        }
        return "";
    }
}
