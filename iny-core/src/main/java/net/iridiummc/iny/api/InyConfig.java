package net.iridiummc.iny.api;

import net.iridiummc.iny.value.InyList;
import net.iridiummc.iny.value.InySection;
import net.iridiummc.iny.value.InyValue;

import java.util.List;
import java.util.Optional;

/**
 * A parsed immutable INY configuration with dotted navigation and typed decoding.
 * Instances are created by {@link Iny}; consumers should depend only on this interface.
 */
public interface InyConfig {

    /** Returns the immutable root section. */
    InySection root();

    /** Returns and decodes a required dotted path. */
    <T> T get(String path, Class<T> type);

    /** Returns a decoded value, or empty when any requested key is missing. */
    <T> Optional<T> find(String path, Class<T> type);

    /** Tests whether a valid dotted path exists. */
    boolean contains(String path);

    /** Returns the raw value at a required dotted path. */
    InyValue getValue(String path);

    /** Returns the raw value at a dotted path, or empty when a key is missing. */
    Optional<InyValue> findValue(String path);

    /** Returns a required section value. */
    InySection getSection(String path);

    /** Returns a required list value. */
    InyList getList(String path);

    /** Returns a required list whose elements are decoded as the requested type. */
    <T> List<T> getList(String path, Class<T> type);
}
