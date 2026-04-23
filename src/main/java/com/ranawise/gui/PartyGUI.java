package com.ranawise.gui;

import com.ranawise.AdvancedParty;
import com.ranawise.party.Party;
import com.ranawise.party.PartyManager;
import com.ranawise.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public final class PartyGUI {

    public static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "Party";

    private final AdvancedParty plugin;
    private final PartyManager manager;
    private final Messages messages;

    public PartyGUI(AdvancedParty plugin, PartyManager manager, Messages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.messages = messages;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE_PREFIX);
        render(inv, viewer);
        viewer.openInventory(inv);
    }

    public void refresh(Player viewer) {
        Inventory top = viewer.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof Holder) {
            top.clear();
            render(top, viewer);
        }
    }

    private void render(Inventory inv, Player viewer) {
        Party party = manager.getParty(viewer.getUniqueId()).orElse(null);

        ItemStack info = item(Material.NETHER_STAR, ChatColor.AQUA + "Party");
        if (party == null) {
            setLore(info,
                    ChatColor.GRAY + "You are not in a party.",
                    " ",
                    ChatColor.YELLOW + "Click to create one.");
            inv.setItem(4, info);
            inv.setItem(49, item(Material.BARRIER, ChatColor.RED + "Close"));
            return;
        }

        setLore(info,
                ChatColor.GRAY + "Leader: " + ChatColor.WHITE + manager.nameOf(party.getLeaderId()),
                ChatColor.GRAY + "Members: " + ChatColor.WHITE + party.size()
                        + "/" + plugin.getPartyConfig().getMaxPartySize(),
                ChatColor.GRAY + "Friendly fire: "
                        + (party.getSettings().isFriendlyFire() ? ChatColor.RED + "ON" : ChatColor.GREEN + "OFF"),
                ChatColor.GRAY + "Privacy: "
                        + (party.getSettings().isPublic() ? ChatColor.GREEN + "Public" : ChatColor.YELLOW + "Private"));
        inv.setItem(4, info);

        int slot = 9;
        boolean viewerIsLeader = party.isLeader(viewer.getUniqueId());
        for (UUID id : party.getMembers()) {
            if (slot >= 36) break;
            inv.setItem(slot++, memberHead(id, party, viewer, viewerIsLeader));
        }

        inv.setItem(45, toggleItem(Material.IRON_SWORD,
                ChatColor.YELLOW + "Friendly fire",
                party.getSettings().isFriendlyFire(),
                viewerIsLeader));
        inv.setItem(46, toggleItem(Material.ENDER_EYE,
                ChatColor.YELLOW + "Public party",
                party.getSettings().isPublic(),
                viewerIsLeader));
        inv.setItem(48, item(Material.OAK_DOOR, ChatColor.GOLD + "Leave party"));
        if (viewerIsLeader) {
            inv.setItem(50, item(Material.TNT, ChatColor.RED + "Disband party"));
        }
        inv.setItem(53, item(Material.BARRIER, ChatColor.RED + "Close"));
    }

    private ItemStack memberHead(UUID id, Party party, Player viewer, boolean viewerIsLeader) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
        meta.setOwningPlayer(offline);
        String name = manager.nameOf(id);
        boolean leader = party.isLeader(id);
        meta.setDisplayName((leader ? ChatColor.GOLD : ChatColor.AQUA) + name
                + (leader ? ChatColor.GRAY + " (Leader)" : ""));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + (offline.isOnline() ? "Online" : "Offline"));
        if (viewerIsLeader && !id.equals(viewer.getUniqueId())) {
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Left-click: " + ChatColor.WHITE + "Kick");
            lore.add(ChatColor.YELLOW + "Right-click: " + ChatColor.WHITE + "Transfer ownership");
        }
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack toggleItem(Material material, String name, boolean on, boolean enabled) {
        ItemStack stack = item(material, name);
        setLore(stack,
                ChatColor.GRAY + "Status: " + (on ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                " ",
                enabled ? ChatColor.YELLOW + "Click to toggle" : ChatColor.DARK_GRAY + "Leader only");
        return stack;
    }

    private ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private void setLore(ItemStack stack, String... lines) {
        ItemMeta meta = stack.getItemMeta();
        meta.setLore(Arrays.asList(lines));
        stack.setItemMeta(meta);
    }

    public void handleClick(Player viewer, int slot, ClickType click) {
        Party party = manager.getParty(viewer.getUniqueId()).orElse(null);

        if (party == null) {
            if (slot == 4) {
                viewer.closeInventory();
                viewer.performCommand("party create");
            } else if (slot == 49 || slot == 53) {
                viewer.closeInventory();
            }
            return;
        }

        if (slot >= 9 && slot < 36) {
            UUID memberId = memberAtSlot(party, slot);
            if (memberId == null || memberId.equals(viewer.getUniqueId())) return;
            if (!party.isLeader(viewer.getUniqueId())) return;
            String name = manager.nameOf(memberId);
            if (click == ClickType.LEFT) {
                manager.kick(viewer, name);
            } else if (click == ClickType.RIGHT) {
                manager.transfer(viewer, name);
            }
            refresh(viewer);
            return;
        }

        switch (slot) {
            case 45 -> {
                if (manager.toggleFriendlyFire(viewer)) refresh(viewer);
                else messages.send(viewer, "not-leader");
            }
            case 46 -> {
                if (manager.togglePublic(viewer)) refresh(viewer);
                else messages.send(viewer, "not-leader");
            }
            case 48 -> {
                viewer.closeInventory();
                viewer.performCommand("party leave");
            }
            case 50 -> {
                if (party.isLeader(viewer.getUniqueId())) {
                    viewer.closeInventory();
                    viewer.performCommand("party disband");
                }
            }
            case 53 -> viewer.closeInventory();
            default -> {}
        }
    }

    private UUID memberAtSlot(Party party, int slot) {
        int index = slot - 9;
        int i = 0;
        for (UUID id : party.getMembers()) {
            if (i++ == index) return id;
        }
        return null;
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inv;
        @Override public @NotNull Inventory getInventory() { return inv; }
    }
}
