package net.iridiummc.iny.internal.factory;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.factory.InyArguments;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.iridiummc.iny.internal.codec.InyValueAccess;
import net.iridiummc.iny.internal.value.InyCall;

import java.util.Objects;

/** Internal context implementation; factories interact only with its public interface. */
public final class DefaultInyFactoryContext implements InyFactoryContext {

    private final Iny iny;
    private final InyValueAccess values;
    private final InyCall call;
    private final String path;
    private final InyArguments arguments;

    public DefaultInyFactoryContext(Iny iny, InyValueAccess values, InyCall call, String path) {
        this.iny = Objects.requireNonNull(iny, "iny");
        this.values = Objects.requireNonNull(values, "values");
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

    InyValueAccess values() {
        return values;
    }

    @Override
    public <T> T decode(Object value, Class<T> type) {
        return values.decode(value, type, path);
    }

    @Override
    public <T> T resolve(Object value, Class<T> type) {
        return values.resolve(value, type, path);
    }
}
