package net.iridiummc.iny.internal.value;

import java.util.List;
import java.util.Objects;

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
        StringBuilder builder = new StringBuilder();
        builder.append("\n");

        for(InyValue value : values) {
            builder.append(value.toText());
        }

        return builder.toString();
    }
}
