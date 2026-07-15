package net.iridiummc.iny.internal.value;

/** Internal INY boolean node. */
public record InyBoolean(boolean value) implements InyValue {

    @Override
    public String actualType() {
        return InyValueType.BOOLEAN.displayName();
    }
}
