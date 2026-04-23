package com.ranawise;

import com.ranawise.buff.BuffTask;
import com.ranawise.commands.PartyChatCommand;
import com.ranawise.commands.PartyCommand;
import com.ranawise.config.PartyConfig;
import com.ranawise.data.PartyDataStore;
import com.ranawise.data.SqlitePartyDataStore;
import com.ranawise.data.YamlPartyDataStore;
import com.ranawise.gui.PartyGUI;
import com.ranawise.listeners.PartyListener;
import com.ranawise.listeners.PlayerConnectionListener;
import com.ranawise.party.PartyManager;
import com.ranawise.util.ChatToggleManager;
import com.ranawise.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvancedParty extends JavaPlugin {

    private PartyConfig partyConfig;
    private Messages messages;
    private PartyManager partyManager;
    private PartyGUI partyGUI;
    private ChatToggleManager chatToggleManager;
    private PartyDataStore dataStore;
    private BuffTask buffTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.partyConfig = new PartyConfig(getConfig());
        this.messages = new Messages(partyConfig);

        this.dataStore = createDataStore();
        this.dataStore.init();

        this.chatToggleManager = new ChatToggleManager();
        this.partyManager = new PartyManager(this, partyConfig, messages, dataStore);
        this.partyGUI = new PartyGUI(this, partyManager, messages);

        registerCommand("party", new PartyCommand(this, partyManager, partyGUI, chatToggleManager, messages));
        registerCommand("partychat", new PartyChatCommand(partyManager, messages));

        getServer().getPluginManager().registerEvents(
                new PartyListener(this, partyManager, partyGUI, chatToggleManager, messages), this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(partyManager, messages), this);

        this.buffTask = new BuffTask(this, partyManager, partyConfig);
        buffTask.start();

        getLogger().info("AdvancedParty enabled.");
    }

    @Override
    public void onDisable() {
        if (buffTask != null) buffTask.stop();
        if (partyManager != null) partyManager.shutdown();
        if (dataStore != null) dataStore.close();
        getLogger().info("AdvancedParty disabled.");
    }

    private PartyDataStore createDataStore() {
        String type = getConfig().getString("storage.type", "yaml").toLowerCase();
        return switch (type) {
            case "sqlite" -> new SqlitePartyDataStore(this);
            default -> new YamlPartyDataStore(this);
        };
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor exec) cmd.setExecutor(exec);
        if (executor instanceof org.bukkit.command.TabCompleter tab) cmd.setTabCompleter(tab);
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public PartyConfig getPartyConfig() {
        return partyConfig;
    }
}
