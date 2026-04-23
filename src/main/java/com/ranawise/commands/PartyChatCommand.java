package com.ranawise.commands;

import com.ranawise.party.PartyManager;
import com.ranawise.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PartyChatCommand implements CommandExecutor {

    private final PartyManager manager;
    private final Messages messages;

    public PartyChatCommand(PartyManager manager, Messages messages) {
        this.manager = manager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (args.length == 0) {
            messages.sendRaw(player, "&cUsage: /pc <message>");
            return true;
        }
        if (!manager.sendPartyChat(player, String.join(" ", args))) {
            messages.send(player, "not-in-party");
        }
        return true;
    }
}
