package net.iridiummc.iny.factory;

import java.util.Optional;

/** Typed positional access to the arguments of one factory call. */
public interface InyArguments {

    /**
     * Returns the number of supplied arguments.
     *
     * @return argument count
     */
    int size();

    /**
     * Returns one argument as its ordinary Java representation.
     *
     * @param index zero-based argument index
     * @return argument value
     */
    Object value(int index);

    /**
     * Returns and decodes one required argument.
     *
     * @param index zero-based argument index
     * @param type requested Java type
     * @param <T> requested Java type
     * @return decoded argument
     */
    <T> T get(int index, Class<T> type);

    /**
     * Returns and decodes one argument when present.
     *
     * @param index zero-based argument index
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded argument, or empty when the index was not supplied
     */
    <T> Optional<T> find(int index, Class<T> type);

    /**
     * Returns and decodes one argument, or returns a default when it was not supplied.
     *
     * @param index zero-based argument index
     * @param type requested Java type
     * @param defaultValue value returned when the index was not supplied
     * @param <T> requested Java type
     * @return the decoded argument or {@code defaultValue}
     */
    <T> T getOrDefault(int index, Class<T> type, T defaultValue);

    /**
     * Requires an exact argument count.
     *
     * @param count required argument count
     */
    void requireCount(int count);

    /**
     * Requires an argument count within an inclusive range.
     *
     * @param minimum minimum accepted argument count
     * @param maximum maximum accepted argument count
     */
    void requireCountBetween(int minimum, int maximum);
}
