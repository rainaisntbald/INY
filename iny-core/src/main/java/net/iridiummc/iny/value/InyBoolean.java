package net.iridiummc.iny.value;

/** An INY boolean. */
public record InyBoolean(boolean value) implements InyValue {

    @Override
    public InyValueType type() {
        return InyValueType.BOOLEAN;
    }
}
