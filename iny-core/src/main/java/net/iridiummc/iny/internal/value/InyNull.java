package net.iridiummc.iny.internal.value;

/** Internal singleton INY null node. */
public final class InyNull implements InyValue {

    public static final InyNull INSTANCE = new InyNull();

    private InyNull() {
    }

    @Override
    public String actualType() {
        return InyValueType.NULL.displayName();
    }
}
