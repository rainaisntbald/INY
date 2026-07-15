package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

import java.util.Objects;

/** Internal INY string node. */
public record InyString(String value) implements InyValue {

    public InyString {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public InyValueType type() {
        return InyValueType.STRING;
    }
}
