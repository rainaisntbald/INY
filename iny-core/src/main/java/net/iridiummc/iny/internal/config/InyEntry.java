package net.iridiummc.iny.internal.config;

import net.iridiummc.iny.internal.value.InyValue;
import net.iridiummc.iny.source.SourcePosition;

public record InyEntry(InyValue value, SourcePosition start, int length) {
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof InyEntry)) return false;
        if(this == other) return true;
        return value.equals(((InyEntry) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
