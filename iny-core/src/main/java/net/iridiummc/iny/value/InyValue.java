package net.iridiummc.iny.value;

/** A value in an immutable INY configuration tree. */
public sealed interface InyValue
        permits InySection, InyList, InyString, InyInteger, InyDecimal, InyBoolean, InyNull, InyCall {

    /** Returns this value's semantic kind. */
    InyValueType type();
}
