package net.iridiummc.iny.internal.annotation;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyDuplicateFactoryException;
import net.iridiummc.iny.factory.InyArguments;
import net.iridiummc.iny.factory.InyFactory;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRuntimeContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;

/** Adapts explicitly selected annotated methods to the public builder registration APIs. */
public final class AnnotatedMethodRegistrar {

    private AnnotatedMethodRegistrar() {
    }

    /** Registers all annotated methods declared directly by the supplied instance's class. */
    public static void register(Iny.Builder builder, Object instance) {
        Objects.requireNonNull(builder, "builder");
        register(instance, builder::registerFactory);
    }

    /**
     * Adapts declared annotated methods into ordinary registrations and passes them to a registry-owned target.
     * This internal hook lets lifecycle integrations add ownership without duplicating reflection or invocation logic.
     */
    public static void register(
            Object instance,
            Consumer<InyFactoryRegistration<?>> registrationTarget
    ) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(registrationTarget, "registrationTarget");

        Class<?> owner = instance.getClass();
        List<Registration> registrations = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods()) {
            Registration registration = inspect(owner, method);
            if (registration != null) {
                registrations.add(registration);
            }
        }

        for (Registration registration : registrations) {
            register(registrationTarget, instance, registration);
        }
    }

    private static Registration inspect(Class<?> owner, Method method) {
        net.iridiummc.iny.annotation.InyFactory factory =
                method.getAnnotation(net.iridiummc.iny.annotation.InyFactory.class);
        net.iridiummc.iny.annotation.InyProvider provider =
                method.getAnnotation(net.iridiummc.iny.annotation.InyProvider.class);
        net.iridiummc.iny.annotation.InyRunnable runnable =
                method.getAnnotation(net.iridiummc.iny.annotation.InyRunnable.class);

        int annotationCount = (factory == null ? 0 : 1)
                + (provider == null ? 0 : 1)
                + (runnable == null ? 0 : 1);
        if (annotationCount == 0) {
            return null;
        }
        if (annotationCount > 1) {
            throw invalid(owner, method, annotations(factory, provider, runnable),
                    "a method may have at most one INY registration annotation", null);
        }

        RegistrationKind kind;
        String identifierText;
        String annotation;
        if (factory != null) {
            kind = RegistrationKind.FACTORY;
            identifierText = factory.value();
            annotation = annotation("InyFactory", identifierText);
        } else if (provider != null) {
            kind = RegistrationKind.PROVIDER;
            identifierText = provider.value();
            annotation = annotation("InyProvider", identifierText);
        } else {
            kind = RegistrationKind.RUNNABLE;
            identifierText = runnable.value();
            annotation = annotation("InyRunnable", identifierText);
        }

        if (kind == RegistrationKind.RUNNABLE && method.getReturnType() != void.class) {
            throw invalid(owner, method, annotation, "runnable methods must return void", null);
        }
        if (kind != RegistrationKind.RUNNABLE && method.getReturnType() == void.class) {
            throw invalid(owner, method, annotation,
                    kind.displayName + " methods must return a value", null);
        }
        if (kind == RegistrationKind.FACTORY && method.getReturnType().isPrimitive()) {
            throw invalid(owner, method, annotation,
                    "factory methods must return a reference type because INY factory registrations "
                            + "cannot declare primitive result types", null);
        }

        InyIdentifier identifier;
        try {
            identifier = InyIdentifier.parse(identifierText);
        } catch (RuntimeException exception) {
            throw invalid(owner, method, annotation, describe(exception), exception);
        }

        try {
            if (!method.trySetAccessible()) {
                throw invalid(owner, method, annotation,
                        "the method is not accessible to INY reflection; open its package to the INY module",
                        null);
            }
        } catch (SecurityException exception) {
            throw invalid(owner, method, annotation,
                    "the method could not be made accessible: " + describe(exception), exception);
        }
        return new Registration(owner, method, kind, identifier, annotation);
    }

    private static void register(
            Consumer<InyFactoryRegistration<?>> registrationTarget,
            Object instance,
            Registration registration
    ) {
        try {
            InyFactoryRegistration<?> adapted = switch (registration.kind) {
                case FACTORY -> factoryRegistration(instance, registration);
                case PROVIDER -> providerRegistration(instance, registration);
                case RUNNABLE -> runnableRegistration(instance, registration);
            };
            registrationTarget.accept(adapted);
        } catch (InyDuplicateFactoryException exception) {
            // Deliberately preserve the builder's existing duplicate-registration behaviour.
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid(registration.owner, registration.method, registration.annotation,
                    "registration failed: " + describe(exception), exception);
        }
    }

    private static InyFactoryRegistration<?> factoryRegistration(
            Object instance,
            Registration registration
    ) {
        return factoryRegistration(instance, registration, registration.method.getReturnType());
    }

    private static <T> InyFactoryRegistration<T> factoryRegistration(
            Object instance,
            Registration registration,
            Class<T> resultType
    ) {
        Method method = registration.method;
        InyFactory<T> factory = context -> {
            Object result;
            if (acceptsFactoryContext(method)) {
                // This retains the existing advanced factory callback shape, including custom arity handling.
                result = invoke(instance, registration, new Object[]{context});
            } else {
                result = invoke(instance, registration, factoryArguments(context.arguments(), method));
            }
            return resultType.cast(result);
        };
        return new InyFactoryRegistration<>(registration.identifier, resultType, factory);
    }

    private static InyFactoryRegistration<?> providerRegistration(
            Object instance,
            Registration registration
    ) {
        return providerRegistration(instance, registration, boxed(registration.method.getReturnType()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> InyFactoryRegistration<?> providerRegistration(
            Object instance,
            Registration registration,
            Class<T> resultType
    ) {
        Method method = registration.method;
        InyFactory<InyProvider<T>> factory = context -> {
            InyProvider<?>[] arguments = providerArguments(context.arguments(), method);
            return runtime -> resultType.cast(invoke(instance, registration, resolve(arguments, runtime)));
        };
        // InyProvider's result parameter is erased; the runtime provider checks retain the concrete method type.
        return new InyFactoryRegistration(registration.identifier, InyProvider.class, factory);
    }

    private static InyFactoryRegistration<net.iridiummc.iny.runtime.InyRunnable> runnableRegistration(
            Object instance,
            Registration registration
    ) {
        Method method = registration.method;
        InyFactory<net.iridiummc.iny.runtime.InyRunnable> factory = context -> {
            InyProvider<?>[] arguments = providerArguments(context.arguments(), method);
            return runtime -> invoke(instance, registration, resolve(arguments, runtime));
        };
        return new InyFactoryRegistration<>(
                registration.identifier,
                net.iridiummc.iny.runtime.InyRunnable.class,
                factory);
    }

    private static boolean acceptsFactoryContext(Method method) {
        return method.getParameterCount() == 1
                && method.getParameterTypes()[0] == InyFactoryContext.class;
    }

    private static Object[] factoryArguments(InyArguments arguments, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        arguments.requireCount(parameterTypes.length);
        Object[] values = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            values[index] = arguments.get(index, parameterTypes[index]);
        }
        return values;
    }

    private static InyProvider<?>[] providerArguments(InyArguments arguments, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        arguments.requireCount(parameterTypes.length);
        InyProvider<?>[] providers = new InyProvider<?>[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            providers[index] = arguments.getProvider(index, parameterTypes[index]);
        }
        return providers;
    }

    private static Object[] resolve(InyProvider<?>[] providers, InyRuntimeContext runtime) {
        Object[] values = new Object[providers.length];
        for (int index = 0; index < providers.length; index++) {
            values[index] = providers[index].resolve(runtime);
        }
        return values;
    }

    private static Object invoke(Object instance, Registration registration, Object[] arguments) {
        try {
            Object receiver = Modifier.isStatic(registration.method.getModifiers()) ? null : instance;
            return registration.method.invoke(receiver, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(methodName(registration.owner, registration.method)
                    + " threw a checked exception", cause);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(methodName(registration.owner, registration.method)
                    + " became inaccessible after registration", exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Could not invoke "
                    + methodName(registration.owner, registration.method)
                    + " with its resolved INY arguments: " + describe(exception), exception);
        }
    }

    private static String annotations(
            net.iridiummc.iny.annotation.InyFactory factory,
            net.iridiummc.iny.annotation.InyProvider provider,
            net.iridiummc.iny.annotation.InyRunnable runnable
    ) {
        StringJoiner joiner = new StringJoiner(", ");
        if (factory != null) joiner.add(annotation("InyFactory", factory.value()));
        if (provider != null) joiner.add(annotation("InyProvider", provider.value()));
        if (runnable != null) joiner.add(annotation("InyRunnable", runnable.value()));
        return joiner.toString();
    }

    private static String annotation(String name, String identifier) {
        return "@" + name + "(\"" + identifier + "\")";
    }

    private static IllegalArgumentException invalid(
            Class<?> owner,
            Method method,
            String annotation,
            String reason,
            Throwable cause
    ) {
        String message = "Invalid annotated INY method " + methodName(owner, method)
                + " for " + annotation + ": " + reason;
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }

    private static String methodName(Class<?> owner, Method method) {
        StringJoiner parameters = new StringJoiner(", ");
        for (Class<?> parameterType : method.getParameterTypes()) {
            parameters.add(parameterType.getTypeName());
        }
        return owner.getTypeName() + "#" + method.getName() + "(" + parameters + ")";
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getTypeName() : message;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private enum RegistrationKind {
        FACTORY("factory"),
        PROVIDER("provider"),
        RUNNABLE("runnable");

        private final String displayName;

        RegistrationKind(String displayName) {
            this.displayName = displayName;
        }
    }

    private record Registration(
            Class<?> owner,
            Method method,
            RegistrationKind kind,
            InyIdentifier identifier,
            String annotation
    ) {
    }
}
