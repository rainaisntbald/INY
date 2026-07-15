package net.iridiummc.iny.internal.value;

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
}
