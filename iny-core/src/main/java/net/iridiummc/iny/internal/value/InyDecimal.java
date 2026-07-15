package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

import java.math.BigDecimal;
import java.util.Objects;

/** Internal arbitrary-precision decimal node. */
public record InyDecimal(BigDecimal value) implements InyValue {

    public InyDecimal {
        Objects.requireNonNull(value, "value");
        value = value.stripTrailingZeros();
    }

    @Override
    public InyValueType type() {
        return InyValueType.DECIMAL;
    }
}
