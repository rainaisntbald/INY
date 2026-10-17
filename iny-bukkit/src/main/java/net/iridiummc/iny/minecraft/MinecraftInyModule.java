package net.iridiummc.iny.minecraft;

import net.iridiummc.iny.annotation.InyFactory;
import net.iridiummc.iny.annotation.InyRunnable;
import net.iridiummc.iny.api.Iny;
import net.iridiummc.iny.api.InyModule;
import net.iridiummc.iny.factory.InyFactoryContext;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
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
        builder.registerAnnotated(this);
    }

    @InyFactory("minecraft:world")
    private World worldFactory(InyFactoryContext context) {
        context.arguments().requireCount(1);
        return world(context.arguments().get(0, String.class));
    }

    @InyFactory("minecraft:material")
    private Material materialFactory(InyFactoryContext context) {
        context.arguments().requireCount(1);
        String name = context.arguments().get(0, String.class);
        Material material = Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown Bukkit material '" + name + "'");
        }
        return material;
    }

    @InyFactory("minecraft:vector")
    private Vector vectorFactory(InyFactoryContext context) {
        context.arguments().requireCount(3);
        return new Vector(
                context.arguments().get(0, Double.class),
                context.arguments().get(1, Double.class),
                context.arguments().get(2, Double.class));
    }

    @InyFactory("minecraft:location")
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

    @InyFactory("minecraft:block")
    private Block blockFactory(InyFactoryContext context) {
        context.arguments().requireCount(4);
        World world = world(context.arguments().get(0, String.class));
        return world.getBlockAt(
                context.arguments().get(1, Integer.class),
                context.arguments().get(2, Integer.class),
                context.arguments().get(3, Integer.class));
    }

    @InyFactory("minecraft:item_stack")
    private ItemStack itemStackFactory(InyFactoryContext context) {
        context.arguments().requireCountBetween(1, 2);
        String name = context.arguments().get(0, String.class);
        Material material = Material.matchMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown Bukkit material '" + name + "'");
        }

        Integer count = context.arguments().getOrDefault(1, Integer.class, 1);

        if(count < 1 || count > 99) throw new IllegalArgumentException("Invalid stack size: " + count);

        return new ItemStack(material, count);
    }

    @InyRunnable("minecraft:send_message")
    private void sendMessage(Player player, Component message) {
        player.sendMessage(message);
    }

    @InyRunnable("minecraft:broadcast")
    private void broadcast(Component message) {
        server.broadcast(message);
    }

    @InyRunnable("minecraft:give_item")
    private void giveItem(Player player, ItemStack itemStack) {
        player.give(itemStack);
    }

    @InyRunnable("minecraft:play_sound")
    private void playSound(Player player, Sound sound) {
        player.playSound(sound);
    }

    @InyRunnable("minecraft:set_block")
    private void setBlock(Location location, Material material) {
        World world = location.getWorld();
        if(world == null) {
            throw new IllegalArgumentException("Location " + location + " has no world");
        }
        world.setType(location, material);
    }

    private World world(String name) {
        World world = server.getWorld(name);
        if (world == null) {
            throw new IllegalArgumentException("Unknown Bukkit world '" + name + "'");
        }
        return world;
    }
}
