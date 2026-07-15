package net.iridiummc.iny.internal.value;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.source.SourcePosition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Internal immutable, unevaluated factory-call node. */
public final class InyCall implements InyValue {

    private final InyIdentifier identifier;
    private final List<InyValue> arguments;
    private final SourcePosition position;

    public InyCall(InyIdentifier identifier, List<InyValue> arguments) {
        this(identifier, arguments, null);
    }

    public InyCall(InyIdentifier identifier, List<InyValue> arguments, SourcePosition position) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        this.position = position;
    }

    public InyIdentifier identifier() {
        return identifier;
    }

    public List<InyValue> arguments() {
        return arguments;
    }

    public Optional<SourcePosition> position() {
        return Optional.ofNullable(position);
    }

    @Override
    public String actualType() {
        return InyValueType.CALL.displayName();
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
}
