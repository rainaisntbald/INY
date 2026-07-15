package net.iridiummc.iny.factory;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyIdentifier;

/** Path-aware services supplied to a registered factory. */
public interface InyFactoryContext {

    /**
     * Returns the identifier of the factory being invoked.
     *
     * @return factory identifier
     */
    InyIdentifier identifier();

    /**
     * Returns the configuration path containing the call.
     *
     * @return current configuration path
     */
    String path();

    /**
     * Returns typed access to the call's positional arguments.
     *
     * @return call arguments
     */
    InyArguments arguments();

    /**
     * Returns the immutable INY service resolving this call.
     *
     * @return owning service
     */
    Iny iny();

    /**
     * Decodes an ordinary value without evaluating factory calls.
     *
     * @param value ordinary value to decode
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the decoded value
     */
    <T> T decode(Object value, Class<T> type);

    /**
     * Decodes a value, evaluating it first when it is a factory call.
     *
     * @param value value or factory call to resolve
     * @param type requested Java type
     * @param <T> requested Java type
     * @return the resolved value
     */
    <T> T resolve(Object value, Class<T> type);
}
