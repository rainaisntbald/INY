package net.iridiummc.iny.api;

import net.iridiummc.iny.exception.InyMissingValueException;
import net.iridiummc.iny.exception.InyPathTraversalException;
import net.iridiummc.iny.internal.path.InyPath;
import net.iridiummc.iny.value.InyList;
import net.iridiummc.iny.value.InySection;
import net.iridiummc.iny.value.InyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A parsed immutable INY configuration with dotted navigation and typed decoding. */
public final class InyConfig {

    private final Iny iny;
    private final InySection root;

    InyConfig(Iny iny, InySection root) {
        this.iny = Objects.requireNonNull(iny, "iny");
        this.root = Objects.requireNonNull(root, "root");
    }

    /** Returns the immutable root section. */
    public InySection root() {
        return root;
    }

    /** Returns and decodes a required dotted path. */
    public <T> T get(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return iny.resolveValue(resolve(path, true).orElseThrow(), type, path);
    }

    /** Returns a decoded value, or empty when any requested key is missing. */
    public <T> Optional<T> find(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return resolve(path, false).map(value -> iny.resolveValue(value, type, path));
    }

    /** Tests whether a valid dotted path exists. */
    public boolean contains(String path) {
        return resolve(path, false).isPresent();
    }

    /** Returns the raw value at a required dotted path. */
    public InyValue getValue(String path) {
        return resolve(path, true).orElseThrow();
    }

    /** Returns the raw value at a dotted path, or empty when a key is missing. */
    public Optional<InyValue> findValue(String path) {
        return resolve(path, false);
    }

    /** Returns a required section value. */
    public InySection getSection(String path) {
        return get(path, InySection.class);
    }

    /** Returns a required list value. */
    public InyList getList(String path) {
        return get(path, InyList.class);
    }

    /** Returns a required list whose elements are decoded as the requested type. */
    public <T> List<T> getList(String path, Class<T> type) {
        Objects.requireNonNull(type, "type");
        InyList list = getList(path);
        ArrayList<T> decoded = new ArrayList<>(list.values().size());
        for (int index = 0; index < list.values().size(); index++) {
            decoded.add(iny.resolveValue(list.values().get(index), type, path + "[" + index + "]"));
        }
        return decoded;
    }

    private Optional<InyValue> resolve(String pathText, boolean required) {
        InyPath path = InyPath.parse(pathText);
        InySection section = root;

        for (int index = 0; index < path.segments().size(); index++) {
            String segment = path.segments().get(index);
            InyValue value = section.entries().get(segment);
            boolean finalSegment = index == path.segments().size() - 1;
            if (value == null) {
                if (required) {
                    throw new InyMissingValueException(pathText, segment, index, finalSegment);
                }
                return Optional.empty();
            }
            if (finalSegment) {
                return Optional.of(value);
            }
            if (!(value instanceof InySection childSection)) {
                throw new InyPathTraversalException(pathText, segment, value.type());
            }
            section = childSection;
        }
        throw new AssertionError("Validated paths always contain at least one segment");
    }
}
