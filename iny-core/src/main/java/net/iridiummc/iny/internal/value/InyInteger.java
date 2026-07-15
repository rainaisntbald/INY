package net.iridiummc.iny.internal.value;

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
    public String actualType() {
        return InyValueType.INTEGER.displayName();
    }
}
