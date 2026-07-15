package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

import java.util.List;
import java.util.Objects;

/** Internal immutable list node. */
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
