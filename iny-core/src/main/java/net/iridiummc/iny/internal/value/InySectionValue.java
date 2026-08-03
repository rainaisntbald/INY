package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.internal.Utils;

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

    public InySectionValue set(String path, InyValue newValue) {
        List<String> segments = Utils.segments(path);
        return setRecursive(segments, 0, newValue, path);
    }

    private InySectionValue setRecursive(List<String> segments, int index, InyValue newValue, String path) {
        String segment = segments.get(index);

        if(index == segments.size() - 1) {
            if(!entries.containsKey(segment)) throw new InyMissingValueException(path, segment, index, true);
            LinkedHashMap<String, InyValue> newSectionEntries = new LinkedHashMap<>(this.entries);
            newSectionEntries.put(segment, newValue);
            return new InySectionValue(newSectionEntries);
        }

        if(!entries.containsKey(segment)) throw new InyMissingValueException(path, segment, index, false);
        if(!(entries.get(segment) instanceof InySectionValue childSection)) throw new InyPathTraversalException(path, segment, entries.get(segment).actualType());

        LinkedHashMap<String, InyValue> newSectionEntries = new LinkedHashMap<>(this.entries);
        newSectionEntries.put(segment, childSection.setRecursive(segments, index + 1, newValue, path));
        return new InySectionValue(newSectionEntries);
    }
}
