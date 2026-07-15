package net.iridiummc.iny.value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** An insertion-ordered immutable mapping of keys to INY values. */
public record InySection(Map<String, InyValue> entries) implements InyValue {

    public InySection {
        Objects.requireNonNull(entries, "entries");
        LinkedHashMap<String, InyValue> copy = new LinkedHashMap<>();
        entries.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "section key"),
                Objects.requireNonNull(value, "section value")));
        entries = Collections.unmodifiableMap(copy);
    }

    public Optional<InyValue> find(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(entries.get(key));
    }

    public InyValue get(String key) {
        Objects.requireNonNull(key, "key");
        InyValue value = entries.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Section has no key '" + key + "'");
        }
        return value;
    }

    @Override
    public InyValueType type() {
        return InyValueType.SECTION;
    }
}
