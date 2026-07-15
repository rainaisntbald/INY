package net.iridiummc.iny.factory;

import java.util.Optional;

/** Typed positional access to the arguments of one factory call. */
public interface InyArguments {

    int size();

    Object value(int index);

    <T> T get(int index, Class<T> type);

    <T> Optional<T> find(int index, Class<T> type);

    <T> T getOrDefault(int index, Class<T> type, T defaultValue);

    void requireCount(int count);

    void requireCountBetween(int minimum, int maximum);
}
