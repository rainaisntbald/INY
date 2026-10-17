package net.iridiummc.iny.internal.factory;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.iridiummc.iny.factory.InyFactoryRegistration;
import net.iridiummc.iny.runtime.InyRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Installs factories owned by INY Core itself. */
public final class BuiltInFactories {

    private BuiltInFactories() {
    }

    /** Installs the complete built-in factory set into a new builder registry. */
    public static void install(Map<InyIdentifier, InyFactoryRegistration<?>> factories) {
        Objects.requireNonNull(factories, "factories");
        InyIdentifier identifier = InyIdentifier.parse("core:sequence");
        factories.put(identifier, new InyFactoryRegistration<>(
                identifier,
                InyRunnable.class,
                BuiltInFactories::sequence));
    }

    private static InyRunnable sequence(InyFactoryContext context) {
        ArrayList<InyRunnable> actions = new ArrayList<>(context.arguments().size());
        for (int index = 0; index < context.arguments().size(); index++) {
            actions.add(context.arguments().get(index, InyRunnable.class));
        }
        List<InyRunnable> immutableActions = List.copyOf(actions);
        return runtime -> {
            Objects.requireNonNull(runtime, "runtime context");
            immutableActions.forEach(action -> action.run(runtime));
        };
    }
}
