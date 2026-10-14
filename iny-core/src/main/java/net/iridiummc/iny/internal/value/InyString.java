package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.internal.Utils;
import net.iridiummc.iny.source.SourcePosition;

import java.util.Objects;

/** Internal INY string node. */
public record InyString(String value) implements InyValue {

    public InyString {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String actualType() {
        return InyValueType.STRING.displayName();
    }

    @Override
    public String toText() {
        return '"' + Utils.escapeString(value) + '"';
    }
}
