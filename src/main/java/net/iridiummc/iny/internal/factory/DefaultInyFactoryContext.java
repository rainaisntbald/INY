package net.iridiummc.iny.internal.factory;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.factory.InyArguments;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.iridiummc.iny.value.InyCall;
import net.iridiummc.iny.value.InyValue;

import java.util.Objects;

/** Internal context implementation; factories interact only with its public interface. */
public final class DefaultInyFactoryContext implements InyFactoryContext {

    private final Iny iny;
    private final InyCall call;
    private final String path;
    private final InyArguments arguments;

    public DefaultInyFactoryContext(Iny iny, InyCall call, String path) {
        this.iny = Objects.requireNonNull(iny, "iny");
        this.call = Objects.requireNonNull(call, "call");
        this.path = Objects.requireNonNull(path, "path");
        this.arguments = new DefaultInyArguments(this, call.arguments());
    }

    @Override
    public InyIdentifier identifier() {
        return call.identifier();
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public InyArguments arguments() {
        return arguments;
    }

    @Override
    public Iny iny() {
        return iny;
    }

    @Override
    public <T> T decode(InyValue value, Class<T> type) {
        return iny.decodeValue(value, type, path);
    }

    @Override
    public <T> T resolve(InyValue value, Class<T> type) {
        return iny.resolveValue(value, type, path);
    }
}
