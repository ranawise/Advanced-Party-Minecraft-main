package com.ranawise.config;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;


public final class PartyConfig {

    private final FileConfiguration config;

    private final int maxPartySize;
    private final int inviteExpirySeconds;
    private final int inviteCooldownSeconds;

    private final boolean buffsEnabled;
    private final int buffRadius;
    private final int buffMinMembersNearby;
    private final int buffIntervalTicks;
    private final List<PotionEffect> buffEffects = new ArrayList<>();

    private final boolean bossBarEnabled;
    private final BarColor bossBarColor;
    private final BarStyle bossBarStyle;

    private final boolean defaultFriendlyFire;
    private final boolean defaultPublic;

    public PartyConfig(FileConfiguration config) {
        this.config = config;

        this.maxPartySize = config.getInt("max-party-size", 5);
        this.inviteExpirySeconds = config.getInt("invite-expiry-seconds", 60);
        this.inviteCooldownSeconds = config.getInt("invite-cooldown-seconds", 5);

        this.buffsEnabled = config.getBoolean("buffs.enabled", true);
        this.buffRadius = config.getInt("buffs.radius", 16);
        this.buffMinMembersNearby = config.getInt("buffs.min-members-nearby", 2);
        this.buffIntervalTicks = config.getInt("buffs.interval-ticks", 60);
        loadBuffEffects(config.getList("buffs.effects"));

        this.bossBarEnabled = config.getBoolean("boss-bar.enabled", true);
        this.bossBarColor = parseEnum(BarColor.class, config.getString("boss-bar.color"), BarColor.BLUE);
        this.bossBarStyle = parseEnum(BarStyle.class, config.getString("boss-bar.style"), BarStyle.SOLID);

        this.defaultFriendlyFire = config.getBoolean("defaults.friendly-fire", false);
        this.defaultPublic = config.getBoolean("defaults.public", false);
    }

    @SuppressWarnings("unchecked")
    private void loadBuffEffects(Object raw) {
        if (!(raw instanceof List<?> list)) return;
        for (Object entry : list) {
            if (!(entry instanceof java.util.Map<?, ?> map)) continue;
            Object typeObj = map.get("type");
            if (typeObj == null) continue;
            PotionEffectType type = PotionEffectType.getByName(typeObj.toString().toUpperCase());
            if (type == null) continue;
            int amplifier = asInt(map.get("amplifier"), 0);
            int duration = asInt(map.get("duration-ticks"), 100);
            buffEffects.add(new PotionEffect(type, duration, amplifier, true, false, false));
        }
    }

    private int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        if (name == null) return fallback;
        try {
            return Enum.valueOf(type, name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public int getMaxPartySize() { return maxPartySize; }
    public int getInviteExpirySeconds() { return inviteExpirySeconds; }
    public int getInviteCooldownSeconds() { return inviteCooldownSeconds; }
    public boolean isBuffsEnabled() { return buffsEnabled; }
    public int getBuffRadius() { return buffRadius; }
    public int getBuffMinMembersNearby() { return buffMinMembersNearby; }
    public int getBuffIntervalTicks() { return buffIntervalTicks; }
    public List<PotionEffect> getBuffEffects() { return buffEffects; }
    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public BarColor getBossBarColor() { return bossBarColor; }
    public BarStyle getBossBarStyle() { return bossBarStyle; }
    public boolean isDefaultFriendlyFire() { return defaultFriendlyFire; }
    public boolean isDefaultPublic() { return defaultPublic; }

    public ConfigurationSection getMessagesSection() {
        return config.getConfigurationSection("messages");
    }
}
