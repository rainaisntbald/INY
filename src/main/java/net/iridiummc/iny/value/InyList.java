package net.iridiummc.iny.value;

import java.util.List;
import java.util.Objects;

/** An immutable ordered list of INY values. */
public record InyList(List<InyValue> values) implements InyValue {

    public InyList {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }

    @Override
    public InyValueType type() {
        return InyValueType.LIST;
    }
}
