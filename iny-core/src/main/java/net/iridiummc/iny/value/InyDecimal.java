package net.iridiummc.iny.value;

import java.math.BigDecimal;
import java.util.Objects;

/** An arbitrary-precision decimal INY number. */
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
