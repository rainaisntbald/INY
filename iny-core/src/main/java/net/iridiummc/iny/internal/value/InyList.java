package net.iridiummc.iny.internal.value;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/** Internal immutable list node. */
public record InyList(List<InyValue> values) implements InyValue {

    public InyList {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }

    @Override
    public String actualType() {
        return InyValueType.LIST.displayName();
    }

    @Override
    public String toText() {
        StringJoiner joiner = new StringJoiner(", ");
        for(InyValue value : values) {
            joiner.add(value.toText());
        }

        return "[" + joiner + "]";
    }
}
