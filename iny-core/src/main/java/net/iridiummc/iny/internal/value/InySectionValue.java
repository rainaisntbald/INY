package net.iridiummc.iny.internal.value;

import java.util.*;

/** Internal immutable section node. */
public record InySectionValue(Map<String, InyValue> entries) implements InyValue {

    public InySectionValue {
        Objects.requireNonNull(entries, "entries");
        LinkedHashMap<String, InyValue> copy = new LinkedHashMap<>();
        entries.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "section key"),
                Objects.requireNonNull(value, "section value")));
        entries = Collections.unmodifiableMap(copy);
    }

    @Override
    public String actualType() {
        return InyValueType.SECTION.displayName();
    }

    @Override
    public String toText() {
        throw new IllegalArgumentException("Cannot convert InySectionValue to text in v1.1.0.");
    }
}
