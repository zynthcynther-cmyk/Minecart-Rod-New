package com.minecartorbital;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RodListener implements Listener {

    private static final long COOLDOWN_MS = 1500;

    private final MinecartOrbital plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public RodListener(MinecartOrbital plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        ItemStack rod;
        if (plugin.isOrbitalRod(main)) {
            rod = main;
        } else if (plugin.isOrbitalRod(off)) {
            rod = off;
        } else {
            return;
        }

        // Strike when the hook hits the ground, hooks an entity, or is reeled in
        PlayerFishEvent.State state = event.getState();
        if (state != PlayerFishEvent.State.IN_GROUND
                && state != PlayerFishEvent.State.CAUGHT_ENTITY
                && state != PlayerFishEvent.State.REEL_IN) {
            return;
        }

        if (!player.hasPermission("minecartorbital.rod")) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        Location target = event.getHook().getLocation();
        plugin.strike(target, plugin.getRodCount(rod), plugin.getRodRadius(rod));
    }
}
