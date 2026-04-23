package com.ranawise.util;

import com.ranawise.config.PartyConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;


public final class Messages {

    private final ConfigurationSection section;
    private final String prefix;

    public Messages(PartyConfig partyConfig) {
        this.section = partyConfig.getMessagesSection();
        this.prefix = color(raw("prefix", ""));
    }

    private String raw(String key, String fallback) {
        return section == null ? fallback : section.getString(key, fallback);
    }

    public String get(String key, Map<String, String> placeholders) {
        String value = raw(key, key);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                value = value.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return color(value);
    }

    public String get(String key) {
        return get(key, null);
    }

    public void send(CommandSender to, String key) {
        send(to, key, null);
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        to.sendMessage(prefix + get(key, placeholders));
    }

    public void sendRaw(CommandSender to, String message) {
        to.sendMessage(prefix + color(message));
    }

    public String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public String prefix() {
        return prefix;
    }

    public static Map<String, String> placeholders(String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("placeholders requires key/value pairs");
        Map<String, String> map = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }
}
