package net.iridiummc.iny;

import net.iridiummc.iny.api.InyIdentifier;
import net.iridiummc.iny.exception.InyInvalidIdentifierException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InyIdentifierTest {

    @Test
    void parsesAndFormatsNamespacedIdentifiers() {
        InyIdentifier identifier = InyIdentifier.parse("myplugin:tools/diamond-hammer.v2");
        assertEquals("myplugin", identifier.namespace());
        assertEquals("tools/diamond-hammer.v2", identifier.value());
        assertEquals("myplugin:tools/diamond-hammer.v2", identifier.toString());
        assertEquals(new InyIdentifier("myplugin", "tools/diamond-hammer.v2"), identifier);
    }

    @Test
    void rejectsInvalidIdentifiers() {
        assertThrows(InyInvalidIdentifierException.class, () -> InyIdentifier.parse("missing_namespace"));
        assertThrows(InyInvalidIdentifierException.class, () -> InyIdentifier.parse("Upper:item"));
        assertThrows(InyInvalidIdentifierException.class, () -> InyIdentifier.parse("valid:Upper"));
        assertThrows(InyInvalidIdentifierException.class, () -> InyIdentifier.parse("valid:path//item"));
        assertThrows(InyInvalidIdentifierException.class, () -> InyIdentifier.parse("valid:"));
    }
}
