package net.iridiummc.iny.internal;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.runtime.InyProvider;

import java.util.Objects;

public class Utils {
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
                    // Just in case of weird Unicode characters
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

    public static void requireAvailableFactoryIdentifier(InyIdentifier identifier, Class<?> resultType) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(resultType, "resultType");
        if (identifier.namespace().equals("core")) {
            throw new IllegalArgumentException("The 'core' namespace is reserved for built-in INY factories");
        }
        if (identifier.namespace().equals("context")
                && (!identifier.value().equals("value")
                || !InyProvider.class.isAssignableFrom(resultType))) {
            throw new IllegalArgumentException("The 'context' namespace is reserved for the context:value provider");
        }
    }

}
