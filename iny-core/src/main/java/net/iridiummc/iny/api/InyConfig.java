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

    /** Sets the value of the path to a {@code String} value */
    InyConfig setString(String path, String newValue);

    /** Sets the value of the path to a {@code Boolean} value */
    InyConfig setBoolean(String path, Boolean newValue);


    /** Sets the value of the path to a {@code BigInteger} value */
    InyConfig setInteger(String path, BigInteger newValue);

    /** Sets the value of the path to an {@code Integer} value */
    default InyConfig setInteger(String path, Integer newValue) {
        return setInteger(path, BigInteger.valueOf(newValue));
    }

    /** Sets the value of the path to a {@code Long} value */
    default InyConfig setInteger(String path, Long newValue) {
        return setInteger(path, BigInteger.valueOf(newValue));
    }


    /** Sets the value of the path to a {@code BigDecimal} value */
    InyConfig setDecimal(String path, BigDecimal newValue);

    /** Sets the value of the path to a {@code Double} value */
    default InyConfig setDecimal(String path, Double newValue) {
        return setDecimal(path, BigDecimal.valueOf(newValue));
    }

    /** Sets the value of the path to a {@code Float} value */
    default InyConfig setDecimal(String path, Float newValue) {
        return setDecimal(path, BigDecimal.valueOf(newValue));
    }


    /** Sets the value of the path to a {@code List<String>} value */
    InyConfig setStringList(String path, List<String> newValue);

    /** Sets the value of the path to a {@code List<Boolean>} list value */
    InyConfig setBooleanList(String path, List<Boolean> newValue);


    /** Sets the value of the path to a {@code List<BigInteger>} value */
    InyConfig setBigIntegerList(String path, List<BigInteger> newValue);

    /** Sets the value of the path to a {@code List<Integer>} value */
    default InyConfig setIntegerList(String path, List<Integer> newValue) {
        return setBigIntegerList(
                path,
                newValue.stream().map(BigInteger::valueOf).toList()
        );
    }

    /** Sets the value of the path to an {@code List<Long>} list value */
    default InyConfig setLongList(String path, List<Long> newValue) {
        return setBigIntegerList(
                path,
                newValue.stream().map(BigInteger::valueOf).toList()
        );
    }


    /** Sets the value of the path to a {@code List<BigDecimal>} value */
    InyConfig setBigDecimalList(String path, List<BigDecimal> newValue);

    /** Sets the value of the path to a {@code List<Double>} value */
    default InyConfig setDoubleList(String path, List<Double> newValue) {
        return setBigDecimalList(path, newValue.stream().map(BigDecimal::valueOf).toList());
    }

    /** Sets the value of the path to a {@code List<Float>} value */
    default InyConfig setFloatList(String path, List<Float> newValue) {
        return setBigDecimalList(path, newValue.stream().map(BigDecimal::valueOf).toList());
    }

    /** Sets the value of the path to {@code null} */
    InyConfig setNull(String path);

    void save(Path path) throws IOException;
}
