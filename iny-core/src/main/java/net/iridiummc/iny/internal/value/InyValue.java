package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.value.InyValueType;

/** Internal semantic node in a parsed INY tree. */
public sealed interface InyValue
        permits InySectionValue, InyList, InyString, InyInteger, InyDecimal, InyBoolean, InyNull, InyCall {

    InyValueType type();
}
