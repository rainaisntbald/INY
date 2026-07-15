package net.iridiummc.iny.internal.path;

import net.iridiummc.iny.exception.InyInvalidPathException;

import java.util.List;
import java.util.regex.Pattern;

/** Internal validated representation of a public dotted lookup path. */
public record InyPath(String text, List<String> segments) {

    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");

    public static InyPath parse(String path) {
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
            if (!SEGMENT.matcher(segment).matches()) {
                throw new InyInvalidPathException(path,
                        "segment " + (index + 1) + " ('" + segment + "') is not a valid bare key");
            }
        }
        return new InyPath(path, segments);
    }
}
