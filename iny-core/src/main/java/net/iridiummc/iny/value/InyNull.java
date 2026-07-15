package net.iridiummc.iny.value;

/** The singleton INY null value. */
public final class InyNull implements InyValue {

    public static final InyNull INSTANCE = new InyNull();

    private InyNull() {
    }

    @Override
    public InyValueType type() {
        return InyValueType.NULL;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof InyNull;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "InyNull";
    }
}
