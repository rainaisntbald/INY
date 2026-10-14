package net.iridiummc.iny.internal;

import net.iridiummc.iny.exception.InyInvalidPathException;

import java.util.List;

public class Utils {
    public static List<String> segments(String path) {
        if (path == null) {
            throw new InyInvalidPathException("null", "path must not be null");
        }
        if (path.isEmpty()) {
            throw new InyInvalidPathException(path, "path must contain at least one segment");
        }
        if (!path.equals(path.trim())) {
            throw new InyInvalidPathException(path, "leading or trailing whitespace is not allowed");
        }

        List<String> segments = List.of(path.split("\\.", -1));
        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            if (!segment.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                throw new InyInvalidPathException(path,
                        "segment " + (index + 1) + " ('" + segment + "') is not a valid bare key");
            }
        }
        return segments;
    }

    public static String escapeString(String string) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < string.length(); i++) {
            char currentChar = string.charAt(i);
            switch(currentChar) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    // Just in case of weird unicode characters
                    if (currentChar < 0x20 || (currentChar >= 0x7F && currentChar <= 0x9F)) {
                        sb.append(String.format("\\u%04x", (int) currentChar));
                    } else {
                        sb.append(currentChar);
                    }
                }
            }
        }

        return sb.toString();
    }

}
