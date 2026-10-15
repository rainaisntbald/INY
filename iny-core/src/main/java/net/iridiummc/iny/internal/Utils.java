package net.iridiummc.iny.internal;

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

}
