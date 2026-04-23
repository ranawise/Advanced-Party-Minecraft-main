package com.ranawise.data;

import com.ranawise.party.Party;
import com.ranawise.party.PartySettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class YamlPartyDataStore implements PartyDataStore {

    private final JavaPlugin plugin;
    private File file;
    private YamlConfiguration yaml;

    public YamlPartyDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "parties.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void close() {
        save();
    }

    @Override
    public Collection<Party> loadAll() {
        List<Party> parties = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection("parties");
        if (root == null) return parties;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            try {
                UUID id = UUID.fromString(key);
                UUID leader = UUID.fromString(s.getString("leader"));
                PartySettings settings = new PartySettings(
                        s.getBoolean("settings.friendly-fire", false),
                        s.getBoolean("settings.public", false));
                List<UUID> members = new ArrayList<>();
                for (String m : s.getStringList("members")) members.add(UUID.fromString(m));
                parties.add(new Party(id, leader, members, settings));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping malformed party entry: " + key);
            }
        }
        return parties;
    }

    @Override
    public void save(Party party) {
        String base = "parties." + party.getId();
        yaml.set(base + ".leader", party.getLeaderId().toString());
        List<String> members = new ArrayList<>();
        for (UUID m : party.getMembers()) members.add(m.toString());
        yaml.set(base + ".members", members);
        yaml.set(base + ".settings.friendly-fire", party.getSettings().isFriendlyFire());
        yaml.set(base + ".settings.public", party.getSettings().isPublic());
        save();
    }

    @Override
    public void delete(Party party) {
        yaml.set("parties." + party.getId(), null);
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save parties.yml: " + ex.getMessage());
        }
    }
}
