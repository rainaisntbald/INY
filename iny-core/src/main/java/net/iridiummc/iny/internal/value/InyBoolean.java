package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

/** Internal INY boolean node. */
public record InyBoolean(boolean value) implements InyValue {

    @Override
    public InyValueType type() {
        return InyValueType.BOOLEAN;
    }
}
