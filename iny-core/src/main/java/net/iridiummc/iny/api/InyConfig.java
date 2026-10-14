package net.iridiummc.iny.api;

import net.iridiummc.iny.value.InySection;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A parsed immutable INY configuration with dotted navigation and typed decoding.
 * Instances are created by {@link Iny}; consumers should depend only on this interface.
 */
public interface InyConfig extends InySection {

    /**
     * Returns the immutable root section.
     *
     * @return this configuration's root section
     */
    InySection root();

    /** Returns and decodes a required dotted path. */
    <T> T get(String path, Class<T> type);

    /** Returns a decoded value, or empty when any requested key is missing. */
    <T> Optional<T> find(String path, Class<T> type);

    /** Tests whether a valid dotted path exists. */
    boolean contains(String path);

    /**
     * Returns a required dotted path as its ordinary Java representation.
     *
     * @param path dotted path relative to the configuration root
     * @return the resolved Java value
     */
    Object getValue(String path);

    /**
     * Returns a dotted path as its ordinary Java representation, or empty when missing or explicitly null.
     * Use {@link #contains(String)} to distinguish an explicit null from a missing path.
     *
     * @param path dotted path relative to the configuration root
     * @return the resolved Java value, or empty when missing or explicitly null
     */
    Optional<Object> findValue(String path);

    /** Returns a required section value. */
    InySection getSection(String path);

    /** Returns a required immutable list of ordinary Java values. */
    List<Object> getList(String path);

    /** Returns a required immutable list whose elements are decoded as the requested type. */
    <T> List<T> getList(String path, Class<T> type);

    /** Sets the value of the path to a string value */
    InyConfig setString(String path, String newValue);

    /** Sets the value of the path to a boolean value */
    InyConfig setBoolean(String path, Boolean newValue);

    /** Sets the value of the path to an integer value */
    InyConfig setInteger(String path, BigInteger integer);

    /** Sets the value of the path to a decimal value */
    InyConfig setDecimal(String path, BigDecimal decimal);

    /** Sets the value of the path to null */
    InyConfig setNull(String path);

    void save(Path path) throws IOException;

    /** Sets the value of the path to an integer value */
    default InyConfig setInteger(String path, Integer integer) {
        return setInteger(path, BigInteger.valueOf(integer));
    }

    /** Sets the value of the path to an integer value */
    default InyConfig setInteger(String path, Long integer) {
        return setInteger(path, BigInteger.valueOf(integer));
    }

    /** Sets the value of the path to a decimal value */
    default InyConfig setDecimal(String path, Double decimal) {
        return setDecimal(path, BigDecimal.valueOf(decimal));
    }

    /** Sets the value of the path to a decimal value */
    default InyConfig setDecimal(String path, Float decimal) {
        return setDecimal(path, BigDecimal.valueOf(decimal));
    }
}
