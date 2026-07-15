/**
 * Bukkit integration for INY, including the complete core API and implementation.
 */
module net.iridiummc.iny.bukkit {
    requires java.logging;
    requires transitive org.bukkit;
    requires static com.google.common;
    requires static org.jetbrains.annotations;

    exports net.iridiummc.iny;
    exports net.iridiummc.iny.api;
    exports net.iridiummc.iny.codec;
    exports net.iridiummc.iny.exception;
    exports net.iridiummc.iny.factory;
    exports net.iridiummc.iny.readiness;
    exports net.iridiummc.iny.source;
    exports net.iridiummc.iny.value;
}
