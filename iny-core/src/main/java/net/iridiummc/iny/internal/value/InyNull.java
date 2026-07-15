package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

/** Internal singleton INY null node. */
public final class InyNull implements InyValue {

    public static final InyNull INSTANCE = new InyNull();

    private InyNull() {
    }

    @Override
    public InyValueType type() {
        return InyValueType.NULL;
    }
}
