package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

import java.math.BigInteger;
import java.util.Objects;

/** Internal arbitrary-precision integral node. */
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
