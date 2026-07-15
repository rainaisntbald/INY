package net.iridiummc.iny.factory;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.value.InyValue;

/** Path-aware services supplied to a registered factory. */
public interface InyFactoryContext {

    InyIdentifier identifier();

    String path();

    InyArguments arguments();

    Iny iny();

    /** Decodes an ordinary value without evaluating factory calls. */
    <T> T decode(InyValue value, Class<T> type);

    /** Decodes a value, evaluating it first when it is a factory call. */
    <T> T resolve(InyValue value, Class<T> type);
}
