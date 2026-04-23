package com.ranawise.buff;

import com.ranawise.config.PartyConfig;
import com.ranawise.party.Party;
import com.ranawise.party.PartyManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class BuffTask {

    private final JavaPlugin plugin;
    private final PartyManager manager;
    private final PartyConfig config;
    private BukkitTask task;

    public BuffTask(JavaPlugin plugin, PartyManager manager, PartyConfig config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
    }

    public void start() {
        if (!config.isBuffsEnabled()) return;
        if (config.getBuffEffects().isEmpty()) return;
        long interval = Math.max(1, config.getBuffIntervalTicks());
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        int radiusSquared = config.getBuffRadius() * config.getBuffRadius();
        int min = config.getBuffMinMembersNearby();
        List<PotionEffect> effects = config.getBuffEffects();

        for (Party party : manager.allParties()) {
            Set<Player> online = party.getOnlineMembers();
            if (online.size() < min) continue;
            for (Player p : online) {
                int nearby = countNearby(p, online, radiusSquared);
                if (nearby >= min) {
                    Set<PotionEffect> applied = new HashSet<>(effects);
                    for (PotionEffect e : applied) p.addPotionEffect(e, true);
                }
            }
        }
    }

    private int countNearby(Player center, Set<Player> party, int radiusSquared) {
        int count = 0;
        Location loc = center.getLocation();
        for (Player p : party) {
            if (!p.getWorld().equals(center.getWorld())) continue;
            if (p.getLocation().distanceSquared(loc) <= radiusSquared) count++;
        }
        return count;
    }
}
