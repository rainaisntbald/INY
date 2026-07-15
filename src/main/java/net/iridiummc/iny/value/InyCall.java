package net.iridiummc.iny.value;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.source.SourcePosition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** An immutable, unevaluated namespaced factory call. */
public final class InyCall implements InyValue {

    private final InyIdentifier identifier;
    private final List<InyValue> arguments;
    private final SourcePosition position;

    public InyCall(InyIdentifier identifier, List<InyValue> arguments) {
        this(identifier, arguments, null);
    }

    public InyCall(InyIdentifier identifier, List<InyValue> arguments, SourcePosition position) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(arguments, "arguments");
        this.arguments = List.copyOf(arguments);
        this.position = position;
    }

    public InyIdentifier identifier() {
        return identifier;
    }

    public List<InyValue> arguments() {
        return arguments;
    }

    /** Returns the call's source location when it originated in parsed text. */
    public Optional<SourcePosition> position() {
        return Optional.ofNullable(position);
    }

    @Override
    public InyValueType type() {
        return InyValueType.CALL;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof InyCall call
                && identifier.equals(call.identifier)
                && arguments.equals(call.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, arguments);
    }

    @Override
    public String toString() {
        return "InyCall[identifier=" + identifier + ", arguments=" + arguments + "]";
    }
}
