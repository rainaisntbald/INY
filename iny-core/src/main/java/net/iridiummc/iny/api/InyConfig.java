package net.iridiummc.iny.api;

import net.iridiummc.iny.value.InySection;

import java.util.List;
import java.util.Optional;

/**
 * A parsed immutable INY configuration with dotted navigation and typed decoding.
 * Instances are created by {@link Iny}; consumers should depend only on this interface.
 */
public interface InyConfig extends InySection {

    /** Returns the immutable root section. */
    InySection root();

    /** Returns and decodes a required dotted path. */
    <T> T get(String path, Class<T> type);

    /** Returns a decoded value, or empty when any requested key is missing. */
    <T> Optional<T> find(String path, Class<T> type);

    /** Tests whether a valid dotted path exists. */
    boolean contains(String path);

    /** Returns a required dotted path as its ordinary Java representation. */
    Object getValue(String path);

    /**
     * Returns a dotted path as its ordinary Java representation, or empty when missing or explicitly null.
     * Use {@link #contains(String)} to distinguish an explicit null from a missing path.
     */
    Optional<Object> findValue(String path);

    /** Returns a required section value. */
    InySection getSection(String path);

    /** Returns a required immutable list of ordinary Java values. */
    List<Object> getList(String path);

    /** Returns a required immutable list whose elements are decoded as the requested type. */
    <T> List<T> getList(String path, Class<T> type);
}
