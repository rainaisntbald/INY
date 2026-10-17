package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.internal.config.InyEntry;

import java.util.*;

/** Internal immutable section node. */
public record InySectionValue(Map<String, InyEntry> entries) implements InyValue {

    public InySectionValue {
        Objects.requireNonNull(entries, "entries");
        LinkedHashMap<String, InyEntry> copy = new LinkedHashMap<>();
        entries.forEach((key, entry) -> copy.put(
                Objects.requireNonNull(key, "section key"),
                Objects.requireNonNull(entry, "section value")));
        entries = Collections.unmodifiableMap(copy);
    }

    @Override
    public String actualType() {
        return InyValueType.SECTION.displayName();
    }
}
