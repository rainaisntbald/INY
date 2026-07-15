package net.iridiummc.iny;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/**
 * Fired once at the end of server startup, after INY has sealed its factory registry.
 * Configuration may be loaded and factory-backed values may be resolved from this event onward.
 */
public final class InyReadyEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final INY iny;

    InyReadyEvent(INY iny) {
        this.iny = Objects.requireNonNull(iny, "iny");
    }

    /** Returns the ready shared INY plugin instance. */
    public INY iny() {
        return iny;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
