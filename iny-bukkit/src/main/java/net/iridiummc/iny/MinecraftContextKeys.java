package net.iridiummc.iny;

import net.iridiummc.iny.runtime.InyContextKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Common semantic Bukkit values that runtime factories may request from callers. */
public final class MinecraftContextKeys {

    /** The player principally responsible for the runtime action. */
    public static final InyContextKey<Player> PLAYER =
            InyContextKey.of("minecraft:player", Player.class);

    /** The principal location for the runtime action. */
    public static final InyContextKey<Location> LOCATION =
            InyContextKey.of("minecraft:location", Location.class);

    /** The principal world for the runtime action. */
    public static final InyContextKey<World> WORLD =
            InyContextKey.of("minecraft:world", World.class);

    private MinecraftContextKeys() {
    }
}
