package com.ranawise.listeners;

import com.ranawise.AdvancedParty;
import com.ranawise.gui.PartyGUI;
import com.ranawise.party.Party;
import com.ranawise.party.PartyManager;
import com.ranawise.util.ChatToggleManager;
import com.ranawise.util.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class PartyListener implements Listener {

    private final AdvancedParty plugin;
    private final PartyManager manager;
    private final PartyGUI gui;
    private final ChatToggleManager chatToggles;
    private final Messages messages;

    public PartyListener(AdvancedParty plugin, PartyManager manager, PartyGUI gui,
                         ChatToggleManager chatToggles, Messages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
        this.chatToggles = chatToggles;
        this.messages = messages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PartyGUI.Holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        gui.handleClick(player, event.getRawSlot(), event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = asPlayer(event.getEntity());
        Player attacker = asPlayer(event.getDamager());
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (victim == null || attacker == null || victim.equals(attacker)) return;

        Party party = manager.getParty(victim.getUniqueId()).orElse(null);
        if (party == null || !party.contains(attacker.getUniqueId())) return;

        if (!party.getSettings().isFriendlyFire()) {
            event.setCancelled(true);
            messages.send(attacker, "ff-blocked");
        }
    }

    private Player asPlayer(Object entity) {
        return entity instanceof Player p ? p : null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!chatToggles.isToggled(player.getUniqueId())) return;
        if (!manager.isInParty(player.getUniqueId())) return;
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getServer().getScheduler().runTask(plugin,
                () -> manager.sendPartyChat(player, message));
    }
}
