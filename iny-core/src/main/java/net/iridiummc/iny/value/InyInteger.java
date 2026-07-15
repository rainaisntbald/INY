package net.iridiummc.iny.value;

import java.math.BigInteger;
import java.util.Objects;

/** An arbitrary-precision integral INY number. */
public record InyInteger(BigInteger value) implements InyValue {

    public InyInteger {
        Objects.requireNonNull(value, "value");
    }

    public InyInteger(long value) {
        this(BigInteger.valueOf(value));
    }

    @Override
    public InyValueType type() {
        return InyValueType.INTEGER;
    }
}
