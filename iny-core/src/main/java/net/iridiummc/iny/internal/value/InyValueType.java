package net.iridiummc.iny.internal.value;

/** Internal semantic kinds represented by the parsed INY value tree. */
enum InyValueType {
    SECTION("section"),
    LIST("list"),
    STRING("string"),
    INTEGER("integer"),
    DECIMAL("decimal"),
    BOOLEAN("boolean"),
    NULL("null"),
    CALL("factory call");

    private final String displayName;

    InyValueType(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }
}
