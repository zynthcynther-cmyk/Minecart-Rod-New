package com.minecartorbital;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MinecartOrbital extends JavaPlugin {

    private static final int DEFAULT_COUNT = 20;
    private static final int DEFAULT_RADIUS = 10;
    private static final int MAX_COUNT = 200;
    private static final int MAX_RADIUS = 50;
    private static final int SPAWN_HEIGHT = 40;

    private NamespacedKey rodKey;
    private NamespacedKey rodCountKey;
    private NamespacedKey rodRadiusKey;

    @Override
    public void onEnable() {
        rodKey = new NamespacedKey(this, "orbital_rod");
        rodCountKey = new NamespacedKey(this, "rod_count");
        rodRadiusKey = new NamespacedKey(this, "rod_radius");
        getServer().getPluginManager().registerEvents(new RodListener(this), this);
        getLogger().info("Minecart Orbital 1.0.0 enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Minecart Orbital 1.0.0 disabled.");
    }

    public NamespacedKey getRodKey() {
        return rodKey;
    }

    /** Builds the unbreakable Minecart Orbital fishing rod. */
    public ItemStack createRod(int count, int radius) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.displayName(Component.text("Minecart Orbital Rod", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Cast it. Reel it in.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("TNT minecarts rain from the sky.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(count + " minecarts, " + radius + " block radius", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(rodKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(rodCountKey, PersistentDataType.INTEGER, count);
        meta.getPersistentDataContainer().set(rodRadiusKey, PersistentDataType.INTEGER, radius);
        rod.setItemMeta(meta);
        return rod;
    }

    /** Minecart count stored on the rod (falls back to the default). */
    public int getRodCount(ItemStack rod) {
        Integer v = rod.getItemMeta().getPersistentDataContainer().get(rodCountKey, PersistentDataType.INTEGER);
        return v != null ? v : DEFAULT_COUNT;
    }

    /** Strike radius stored on the rod (falls back to the default). */
    public int getRodRadius(ItemStack rod) {
        Integer v = rod.getItemMeta().getPersistentDataContainer().get(rodRadiusKey, PersistentDataType.INTEGER);
        return v != null ? v : DEFAULT_RADIUS;
    }

    /** True if the item is the Minecart Orbital Rod. */
    public boolean isOrbitalRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(rodKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (name.equals("minecartorbitalrod")) {
            if (!player.hasPermission("minecartorbital.rod")) {
                player.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            int rodCount = DEFAULT_COUNT;
            int rodRadius = DEFAULT_RADIUS;
            try {
                if (args.length >= 1) rodCount = Integer.parseInt(args[0]);
                if (args.length >= 2) rodRadius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cUsage: /minecartorbitalrod [count] [radius]");
                return true;
            }
            rodCount = Math.max(1, Math.min(rodCount, MAX_COUNT));
            rodRadius = Math.max(1, Math.min(rodRadius, MAX_RADIUS));

            player.getInventory().addItem(createRod(rodCount, rodRadius)).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.sendMessage("§6You received the §eMinecart Orbital Rod §6(" + rodCount
                    + " minecarts, " + rodRadius + " block radius)!");
            return true;
        }

        if (name.equals("minecartorbital")) {
            if (!player.hasPermission("minecartorbital.use")) {
                player.sendMessage("§cYou don't have permission to do that.");
                return true;
            }

            int count = DEFAULT_COUNT;
            int radius = DEFAULT_RADIUS;

            try {
                if (args.length >= 1) count = Integer.parseInt(args[0]);
                if (args.length >= 2) radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cUsage: /minecartorbital [count] [radius]");
                return true;
            }

            count = Math.max(1, Math.min(count, MAX_COUNT));
            radius = Math.max(1, Math.min(radius, MAX_RADIUS));

            player.sendMessage("§6Minecart Orbital strike incoming: §e" + count + " §6TNT minecarts!");
            strike(player.getLocation(), count, radius);
            return true;
        }

        return false;
    }

    /** Rains TNT minecarts from the sky around the given location. */
    public void strike(Location center, int total, int radius) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }
        final int maxY = world.getMaxHeight() - 2;
        final Location origin = center.clone();

        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                // Spawn a few per tick so the server doesn't choke
                for (int i = 0; i < 3 && spawned < total; i++, spawned++) {
                    ThreadLocalRandom rng = ThreadLocalRandom.current();
                    double x = origin.getX() + rng.nextDouble(-radius, radius);
                    double z = origin.getZ() + rng.nextDouble(-radius, radius);
                    double y = Math.min(origin.getY() + SPAWN_HEIGHT, maxY);

                    Location spawnLoc = new Location(world, x, y, z);
                    ExplosiveMinecart cart = world.spawn(spawnLoc, ExplosiveMinecart.class);
                    cart.setVelocity(new Vector(0, -1.5, 0));
                }
                if (spawned >= total) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    public int getDefaultCount() {
        return DEFAULT_COUNT;
    }

    public int getDefaultRadius() {
        return DEFAULT_RADIUS;
    }
}
