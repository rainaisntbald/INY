package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.source.SourcePosition;

/** Internal semantic node in a parsed INY tree. */
public sealed interface InyValue
        permits InySectionValue, InyList, InyString, InyInteger, InyDecimal, InyBoolean, InyNull, InyCall {

    /** Returns a human-readable description for internal diagnostics. */
    String actualType();

    String toText();
}
