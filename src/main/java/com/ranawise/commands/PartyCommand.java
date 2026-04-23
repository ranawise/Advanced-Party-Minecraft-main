package com.ranawise.commands;

import com.ranawise.AdvancedParty;
import com.ranawise.gui.PartyGUI;
import com.ranawise.party.Party;
import com.ranawise.party.PartyManager;
import com.ranawise.util.ChatToggleManager;
import com.ranawise.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PartyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "invite", "join", "leave", "disband", "kick",
            "transfer", "chat", "ff", "info", "help");

    private final AdvancedParty plugin;
    private final PartyManager manager;
    private final PartyGUI gui;
    private final ChatToggleManager chatToggles;
    private final Messages messages;

    public PartyCommand(AdvancedParty plugin, PartyManager manager, PartyGUI gui,
                        ChatToggleManager chatToggles, Messages messages) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
        this.chatToggles = chatToggles;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(player);
            case "invite" -> handleInvite(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "kick" -> handleKick(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "chat" -> handleChat(player);
            case "ff" -> handleFriendlyFire(player);
            case "info" -> handleInfo(player);
            case "help" -> sendHelp(player);
            default -> messages.send(player, "unknown-subcommand");
        }
        return true;
    }

    private void handleCreate(Player player) {
        if (!player.hasPermission("party.create")) {
            messages.send(player, "no-permission");
            return;
        }
        switch (manager.create(player)) {
            case CREATED -> messages.send(player, "party-created");
            case ALREADY_IN_PARTY -> messages.send(player, "already-in-party");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (!player.hasPermission("party.invite")) {
            messages.send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            messages.sendRaw(player, "&cUsage: /party invite <player>");
            return;
        }
        String name = args[1];
        switch (manager.invite(player, name)) {
            case SENT -> {
                messages.send(player, "invite-sent", Messages.placeholders("player", name));
                Player target = Bukkit.getPlayerExact(name);
                if (target != null) {
                    messages.send(target, "invite-received", Messages.placeholders(
                            "player", player.getName(),
                            "seconds", String.valueOf(plugin.getPartyConfig().getInviteExpirySeconds())));
                }
            }
            case TARGET_OFFLINE -> messages.send(player, "player-not-found");
            case TARGET_IN_PARTY -> messages.send(player, "already-in-party");
            case SELF -> messages.send(player, "cannot-invite-self");
            case FULL -> messages.send(player, "party-full");
            case COOLDOWN -> messages.send(player, "invite-cooldown",
                    Messages.placeholders("seconds",
                            String.valueOf(manager.getInviteCooldownRemaining(player.getUniqueId()))));
            case NOT_IN_PARTY, NOT_LEADER_OR_MEMBER -> messages.send(player, "not-in-party");
        }
    }

    private void handleJoin(Player player, String[] args) {
        String inviter = args.length >= 2 ? args[1] : null;
        switch (manager.join(player, inviter)) {
            case JOINED -> { /* broadcast already sent */ }
            case ALREADY_IN_PARTY -> messages.send(player, "already-in-party");
            case NO_INVITE -> messages.send(player, "invite-not-found");
            case EXPIRED -> messages.send(player, "invite-expired",
                    Messages.placeholders("player", inviter == null ? "" : inviter));
            case FULL -> messages.send(player, "party-full");
            case NOT_FOUND -> messages.send(player, "invite-not-found");
        }
    }

    private void handleLeave(Player player) {
        if (!manager.leave(player.getUniqueId(),
                com.ranawise.api.events.PartyLeaveEvent.Reason.LEAVE)) {
            messages.send(player, "not-in-party");
        }
        chatToggles.clear(player.getUniqueId());
    }

    private void handleDisband(Player player) {
        if (!manager.isInParty(player.getUniqueId())) {
            messages.send(player, "not-in-party");
            return;
        }
        if (!manager.disband(player.getUniqueId())) {
            messages.send(player, "not-leader");
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendRaw(player, "&cUsage: /party kick <player>");
            return;
        }
        switch (manager.kick(player, args[1])) {
            case KICKED -> { /* broadcast handled */ }
            case NOT_IN_PARTY -> messages.send(player, "not-in-party");
            case NOT_LEADER -> messages.send(player, "not-leader");
            case NOT_A_MEMBER -> messages.send(player, "player-not-found");
            case CANNOT_KICK_SELF -> messages.sendRaw(player, "&cYou cannot kick yourself. Use /party leave.");
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendRaw(player, "&cUsage: /party transfer <player>");
            return;
        }
        switch (manager.transfer(player, args[1])) {
            case TRANSFERRED -> { /* broadcast handled */ }
            case NOT_IN_PARTY -> messages.send(player, "not-in-party");
            case NOT_LEADER -> messages.send(player, "not-leader");
            case TARGET_NOT_MEMBER -> messages.send(player, "player-not-found");
        }
    }

    private void handleChat(Player player) {
        if (!manager.isInParty(player.getUniqueId())) {
            messages.send(player, "not-in-party");
            return;
        }
        boolean now = chatToggles.toggle(player.getUniqueId());
        messages.send(player, now ? "chat-mode-on" : "chat-mode-off");
    }

    private void handleFriendlyFire(Player player) {
        if (!manager.isInParty(player.getUniqueId())) {
            messages.send(player, "not-in-party");
            return;
        }
        if (!manager.toggleFriendlyFire(player)) {
            messages.send(player, "not-leader");
        }
    }

    private void handleInfo(Player player) {
        Party party = manager.getParty(player.getUniqueId()).orElse(null);
        if (party == null) {
            messages.send(player, "not-in-party");
            return;
        }
        messages.sendRaw(player, "&8&m----&r &bParty &8&m----");
        messages.sendRaw(player, "&7Leader: &f" + manager.nameOf(party.getLeaderId()));
        String members = party.getMembers().stream()
                .map(manager::nameOf)
                .collect(Collectors.joining(", "));
        messages.sendRaw(player, "&7Members &8(&f" + party.size() + "&8/&f"
                + plugin.getPartyConfig().getMaxPartySize() + "&8): &f" + members);
        messages.sendRaw(player, "&7Friendly fire: " + (party.getSettings().isFriendlyFire() ? "&cON" : "&aOFF"));
    }

    private void sendHelp(Player player) {
        messages.sendRaw(player, messages.get("help-header"));
        String[][] lines = {
                {"party create", "Create a new party"},
                {"party invite <player>", "Invite a player to your party"},
                {"party join <player>", "Accept an invite from a player"},
                {"party leave", "Leave your current party"},
                {"party disband", "Disband your party (leader only)"},
                {"party kick <player>", "Kick a member (leader only)"},
                {"party transfer <player>", "Transfer ownership (leader only)"},
                {"party ff", "Toggle friendly fire (leader only)"},
                {"party chat", "Toggle party-chat mode"},
                {"party info", "Show your party info"},
                {"pc <message>", "Send a message to your party"},
        };
        for (String[] l : lines) {
            messages.sendRaw(player, messages.get("help-line",
                    Messages.placeholders("usage", l[0], "description", l[1])));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "invite" -> {
                    return filter(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName).toList(), args[1]);
                }
                case "join" -> {
                    return filter(manager.getInviterNamesFor(player.getUniqueId()), args[1]);
                }
                case "kick", "transfer" -> {
                    return manager.getParty(player.getUniqueId())
                            .map(p -> filter(p.getMembers().stream()
                                    .filter(id -> !id.equals(player.getUniqueId()))
                                    .map(manager::nameOf).toList(), args[1]))
                            .orElse(Collections.emptyList());
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase().startsWith(lower)) out.add(o);
        return out;
    }

}
