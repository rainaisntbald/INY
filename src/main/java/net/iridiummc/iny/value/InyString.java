package net.iridiummc.iny.value;

import java.util.Objects;

/** An INY string, from either a quoted string or a bare scalar identifier. */
public record InyString(String value) implements InyValue {

    public InyString {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public InyValueType type() {
        return InyValueType.STRING;
    }
}
