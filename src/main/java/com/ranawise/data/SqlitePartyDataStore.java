package com.ranawise.data;

import com.ranawise.party.Party;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;

public final class SqlitePartyDataStore implements PartyDataStore {

    private final JavaPlugin plugin;

    public SqlitePartyDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        plugin.getLogger().info("SQLite store selected — using stub. Implement JDBC to enable persistence.");
    }

    @Override public void close() {}
    @Override public Collection<Party> loadAll() { return Collections.emptyList(); }
    @Override public void save(Party party) {}
    @Override public void delete(Party party) {}
}
