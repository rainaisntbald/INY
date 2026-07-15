package net.iridiummc.iny.internal.codec;

import net.iridiummc.iny.codec.InyDecodeContext;
import net.iridiummc.iny.codec.InyDecoder;
import net.iridiummc.iny.value.InySection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/** Installs the strict built-in decoder set into the same map used for extensions. */
public final class BuiltInDecoders {

    private BuiltInDecoders() {
    }

    public static void install(Map<Class<?>, InyDecoder<?>> decoders) {
        put(decoders, decoder(String.class, (value, context) -> {
            if (value instanceof String string) {
                return string;
            }
            throw context.failure("only INY strings decode as Java strings");
        }));
        put(decoders, decoder(Boolean.class, BuiltInDecoders::decodeBoolean));
        put(decoders, decoder(boolean.class, BuiltInDecoders::decodeBoolean));
        put(decoders, decoder(Integer.class, (value, context) -> integer(value, context).intValueExact()));
        put(decoders, decoder(int.class, (value, context) -> integer(value, context).intValueExact()));
        put(decoders, decoder(Long.class, (value, context) -> integer(value, context).longValueExact()));
        put(decoders, decoder(long.class, (value, context) -> integer(value, context).longValueExact()));
        put(decoders, decoder(Short.class, (value, context) -> integer(value, context).shortValueExact()));
        put(decoders, decoder(short.class, (value, context) -> integer(value, context).shortValueExact()));
        put(decoders, decoder(Byte.class, (value, context) -> integer(value, context).byteValueExact()));
        put(decoders, decoder(byte.class, (value, context) -> integer(value, context).byteValueExact()));
        put(decoders, decoder(Double.class, BuiltInDecoders::decodeDouble));
        put(decoders, decoder(double.class, BuiltInDecoders::decodeDouble));
        put(decoders, decoder(Float.class, BuiltInDecoders::decodeFloat));
        put(decoders, decoder(float.class, BuiltInDecoders::decodeFloat));
        put(decoders, decoder(BigInteger.class, BuiltInDecoders::integer));
        put(decoders, decoder(BigDecimal.class, BuiltInDecoders::decimal));
        put(decoders, decoder(Object.class, (value, context) -> value));
        put(decoders, decoder(InySection.class, (value, context) -> {
            if (value instanceof InySection section) {
                return section;
            }
            throw context.failure("the value is not a section");
        }));
        put(decoders, listDecoder());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static InyDecoder<List> listDecoder() {
        return decoder(List.class, (value, context) -> {
            if (value instanceof List<?> list) {
                return list;
            }
            throw context.failure("the value is not a list");
        });
    }

    private static Boolean decodeBoolean(Object value, InyDecodeContext context) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw context.failure("only INY booleans decode as Java booleans");
    }

    private static BigInteger integer(Object value, InyDecodeContext context) {
        try {
            if (value instanceof BigInteger integer) {
                return integer;
            }
            if (value instanceof BigDecimal decimal) {
                return decimal.toBigIntegerExact();
            }
        } catch (ArithmeticException exception) {
            throw context.failure("fractional values cannot decode as integral Java numbers", exception);
        }
        throw context.failure("only numeric INY values decode as Java numbers");
    }

    private static Double decodeDouble(Object value, InyDecodeContext context) {
        BigDecimal decimal = decimal(value, context);
        double converted = decimal.doubleValue();
        if (!Double.isFinite(converted) || converted == 0.0d && decimal.signum() != 0) {
            throw context.failure("numeric value is outside the finite non-underflowing double range");
        }
        return converted;
    }

    private static Float decodeFloat(Object value, InyDecodeContext context) {
        BigDecimal decimal = decimal(value, context);
        float converted = decimal.floatValue();
        if (!Float.isFinite(converted) || converted == 0.0f && decimal.signum() != 0) {
            throw context.failure("numeric value is outside the finite non-underflowing float range");
        }
        return converted;
    }

    private static BigDecimal decimal(Object value, InyDecodeContext context) {
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        throw context.failure("only numeric INY values decode as Java numbers");
    }

    private static <T> InyDecoder<T> decoder(Class<T> type, DecodeFunction<T> function) {
        return new InyDecoder<>() {
            @Override
            public Class<T> targetType() {
                return type;
            }

            @Override
            public T decode(Object value, InyDecodeContext context) {
                try {
                    return function.decode(value, context);
                } catch (ArithmeticException exception) {
                    throw context.failure("numeric value is outside the range of " + type.getTypeName(), exception);
                }
            }
        };
    }

    private static void put(Map<Class<?>, InyDecoder<?>> decoders, InyDecoder<?> decoder) {
        InyDecoder<?> previous = decoders.putIfAbsent(decoder.targetType(), decoder);
        if (previous != null) {
            throw new IllegalStateException("Duplicate built-in decoder for " + decoder.targetType().getTypeName());
        }
    }

    @FunctionalInterface
    private interface DecodeFunction<T> {
        T decode(Object value, InyDecodeContext context);
    }
}
