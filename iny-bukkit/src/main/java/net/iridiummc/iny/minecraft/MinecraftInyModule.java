package net.iridiummc.iny.minecraft;

import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyModule;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.iridiummc.iny.MinecraftContextKeys;
import net.iridiummc.iny.runtime.InyProvider;
import net.iridiummc.iny.runtime.InyRunnable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Objects;

/** A compact Paper/Bukkit demonstration built entirely on INY's public factory API. */
public final class MinecraftInyModule implements InyModule {

    private final Server server;

    /**
     * Creates a module backed by one Bukkit server.
     *
     * @param server server used to resolve worlds and other Bukkit values
     */
    public MinecraftInyModule(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void configure(Iny.Builder builder) {
        builder.registerContextKey(MinecraftContextKeys.PLAYER);
        builder.registerContextKey(MinecraftContextKeys.LOCATION);
        builder.registerContextKey(MinecraftContextKeys.WORLD);

        builder.registerRunnable("minecraft:send_message", this::sendMessage);
        builder.registerRunnable("minecraft:give_item", this::giveItem);

        builder.registerFactory("minecraft:world", World.class, this::worldFactory);
        builder.registerFactory("minecraft:material", Material.class, this::materialFactory);
        builder.registerFactory("minecraft:vector", Vector.class, this::vectorFactory);
        builder.registerFactory("minecraft:location", Location.class, this::locationFactory);
        builder.registerFactory("minecraft:block", Block.class, this::blockFactory);
        builder.registerFactory("minecraft:item_stack", ItemStack.class, this::itemStackFactory);
    }

    private World worldFactory(InyFactoryContext context) {
        context.arguments().requireCount(1);
        return world(context.arguments().get(0, String.class));
    }

    private Material materialFactory(InyFactoryContext context) {
        context.arguments().requireCount(1);
        String name = context.arguments().get(0, String.class);
        Material material = Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown Bukkit material '" + name + "'");
        }
        return material;
    }

    private Vector vectorFactory(InyFactoryContext context) {
        context.arguments().requireCount(3);
        return new Vector(
                context.arguments().get(0, Double.class),
                context.arguments().get(1, Double.class),
                context.arguments().get(2, Double.class));
    }

    private Location locationFactory(InyFactoryContext context) {
        context.arguments().requireCountBetween(4, 6);
        World world = world(context.arguments().get(0, String.class));
        double x = context.arguments().get(1, Double.class);
        double y = context.arguments().get(2, Double.class);
        double z = context.arguments().get(3, Double.class);
        float yaw = context.arguments().getOrDefault(4, Float.class, 0.0f);
        float pitch = context.arguments().getOrDefault(5, Float.class, 0.0f);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private Block blockFactory(InyFactoryContext context) {
        context.arguments().requireCount(4);
        World world = world(context.arguments().get(0, String.class));
        return world.getBlockAt(
                context.arguments().get(1, Integer.class),
                context.arguments().get(2, Integer.class),
                context.arguments().get(3, Integer.class));
    }

    private ItemStack itemStackFactory(InyFactoryContext context) {
        context.arguments().requireCount(2);
        String name = context.arguments().get(0, String.class);
        Material material = Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown Bukkit material '" + name + "'");
        }

        int count = context.arguments().get(1, Integer.class);
        if(count < 1 || count > 99) throw new IllegalArgumentException("Invalid stack size: " + count);

        return new ItemStack(material, count);
    }

    private InyRunnable sendMessage(InyFactoryContext context) {
        context.arguments().requireCount(2);

        InyProvider<Player> playerProvider =
                context.arguments().getProvider(0, Player.class);
        InyProvider<String> messageProvider =
                context.arguments().getProvider(1, String.class);

        return runtime -> {
            Player player = playerProvider.resolve(runtime);
            String message = messageProvider.resolve(runtime);
            player.sendMessage(message);
        };
    }

    private InyRunnable giveItem(InyFactoryContext context) {
        context.arguments().requireCount(2);

        InyProvider<Player> playerProvider =
                context.arguments().getProvider(0, Player.class);
        InyProvider<ItemStack> itemStackProvider =
                context.arguments().getProvider(1, ItemStack.class);

        return runtime -> {
            Player player = playerProvider.resolve(runtime);
            ItemStack itemStack = itemStackProvider.resolve(runtime);
            player.give(itemStack);
        };
    }

    private World world(String name) {
        World world = server.getWorld(name);
        if (world == null) {
            throw new IllegalArgumentException("Unknown Bukkit world '" + name + "'");
        }
        return world;
    }
}
