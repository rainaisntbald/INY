package net.iridiummc.iny.internal.config;

import net.iridiummc.iny.internal.value.InyValue;
import net.iridiummc.iny.source.SourcePosition;

public record InyEntry(InyValue value, SourcePosition start, int length) {
}
