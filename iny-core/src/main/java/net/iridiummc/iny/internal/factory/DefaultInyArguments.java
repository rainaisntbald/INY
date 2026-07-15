package net.iridiummc.iny.internal.factory;

import net.iridiummc.iny.exception.InyArgumentCountException;
import net.iridiummc.iny.exception.InyFactoryArgumentException;
import net.iridiummc.iny.factory.InyArguments;
import net.iridiummc.iny.internal.value.InyValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Internal immutable typed argument view. */
final class DefaultInyArguments implements InyArguments {

    private final DefaultInyFactoryContext context;
    private final List<InyValue> values;

    DefaultInyArguments(DefaultInyFactoryContext context, List<InyValue> values) {
        this.context = Objects.requireNonNull(context, "context");
        this.values = List.copyOf(values);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Object value(int index) {
        InyValue value = checkedValue(index, Object.class);
        return context.values().resolve(value, Object.class, context.path() + "[" + index + "]");
    }

    @Override
    public <T> T get(int index, Class<T> type) {
        Objects.requireNonNull(type, "type");
        InyValue value = checkedValue(index, type);
        String argumentPath = context.path() + "[" + index + "]";
        try {
            return context.values().resolve(value, type, argumentPath);
        } catch (RuntimeException exception) {
            throw new InyFactoryArgumentException(
                    context.path(),
                    context.identifier(),
                    index,
                    type,
                    value.type(),
                    "argument resolution failed: " + describe(exception),
                    exception);
        }
    }

    @Override
    public <T> Optional<T> find(int index, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (index < 0) {
            throw missing(index, type);
        }
        return index >= values.size() ? Optional.empty() : Optional.ofNullable(get(index, type));
    }

    @Override
    public <T> T getOrDefault(int index, Class<T> type, T defaultValue) {
        return find(index, type).orElse(defaultValue);
    }

    @Override
    public void requireCount(int count) {
        requireCountBetween(count, count);
    }

    @Override
    public void requireCountBetween(int minimum, int maximum) {
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid argument count range " + minimum + " to " + maximum);
        }
        if (values.size() < minimum || values.size() > maximum) {
            throw new InyArgumentCountException(
                    context.path(), context.identifier(), minimum, maximum, values.size());
        }
    }

    private InyValue checkedValue(int index, Class<?> requestedType) {
        if (index < 0 || index >= values.size()) {
            throw missing(index, requestedType);
        }
        return values.get(index);
    }

    private InyFactoryArgumentException missing(int index, Class<?> requestedType) {
        return new InyFactoryArgumentException(
                context.path(),
                context.identifier(),
                index,
                requestedType,
                null,
                "argument index is outside the available range 0 to " + Math.max(-1, values.size() - 1),
                null);
    }

    private static String describe(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getTypeName() : message;
    }
}
