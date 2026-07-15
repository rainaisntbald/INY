package net.iridiummc.iny.api;

import net.iridiummc.iny.exception.InyInvalidIdentifierException;

import java.util.regex.Pattern;

/**
 * Immutable lowercase namespaced identifier for future INY integrations.
 *
 * @param namespace registry namespace
 * @param value namespaced path or value
 */
public record InyIdentifier(String namespace, String value) {

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9][a-z0-9_.-]*");
    private static final Pattern VALUE = Pattern.compile("[a-z0-9][a-z0-9_./-]*");

    /** Validates and creates a namespaced identifier. */
    public InyIdentifier {
        if (namespace == null || value == null) {
            throw new InyInvalidIdentifierException(String.valueOf(namespace) + ":" + value,
                    "namespace and value must not be null");
        }
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new InyInvalidIdentifierException(namespace + ":" + value,
                    "namespace must be lowercase and contain only letters, digits, '_', '-', or '.'");
        }
        if (!VALUE.matcher(value).matches() || value.endsWith("/") || value.contains("//")) {
            throw new InyInvalidIdentifierException(namespace + ":" + value,
                    "value must be a lowercase non-empty path using letters, digits, '_', '-', '.', or '/'");
        }
    }

    /**
     * Parses the canonical {@code namespace:value} representation.
     *
     * @param identifier canonical identifier text
     * @return the parsed identifier
     */
    public static InyIdentifier parse(String identifier) {
        if (identifier == null) {
            throw new InyInvalidIdentifierException("null", "identifier must not be null");
        }
        int colon = identifier.indexOf(':');
        if (colon <= 0 || colon != identifier.lastIndexOf(':') || colon == identifier.length() - 1) {
            throw new InyInvalidIdentifierException(identifier,
                    "expected exactly one ':' separating a non-empty namespace and value");
        }
        return new InyIdentifier(identifier.substring(0, colon), identifier.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + value;
    }
}
