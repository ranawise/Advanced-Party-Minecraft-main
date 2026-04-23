package com.ranawise.party;

import com.ranawise.api.events.PartyCreateEvent;
import com.ranawise.api.events.PartyJoinEvent;
import com.ranawise.api.events.PartyLeaveEvent;
import com.ranawise.config.PartyConfig;
import com.ranawise.data.PartyDataStore;
import com.ranawise.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class PartyManager {

    private final JavaPlugin plugin;
    private final PartyConfig config;
    private final Messages messages;
    private final PartyDataStore dataStore;

    /** partyId -> party */
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    /** playerId -> partyId */
    private final Map<UUID, UUID> playerParty = new ConcurrentHashMap<>();
    /** target -> (inviter -> invite) */
    private final Map<UUID, Map<UUID, PartyInvite>> invites = new ConcurrentHashMap<>();
    /** inviter -> last invite send timestamp (ms) */
    private final Map<UUID, Long> inviteCooldowns = new ConcurrentHashMap<>();
    /** partyId -> boss bar */
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    public PartyManager(JavaPlugin plugin, PartyConfig config, Messages messages, PartyDataStore dataStore) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.dataStore = dataStore;
        loadFromStore();
    }

    private void loadFromStore() {
        for (Party p : dataStore.loadAll()) {
            parties.put(p.getId(), p);
            for (UUID m : p.getMembers()) playerParty.put(m, p.getId());
            createBossBarIfEnabled(p);
        }
    }

    public void shutdown() {
        for (BossBar b : bossBars.values()) b.removeAll();
        bossBars.clear();
    }


    public Optional<Party> getParty(UUID playerId) {
        UUID pid = playerParty.get(playerId);
        if (pid == null) return Optional.empty();
        return Optional.ofNullable(parties.get(pid));
    }

    public boolean isInParty(UUID playerId) { return playerParty.containsKey(playerId); }

    public boolean isLeader(UUID playerId) {
        return getParty(playerId).map(p -> p.isLeader(playerId)).orElse(false);
    }

    public Collection<Party> allParties() { return Collections.unmodifiableCollection(parties.values()); }


    public enum CreateResult { CREATED, ALREADY_IN_PARTY }

    public CreateResult create(Player leader) {
        if (isInParty(leader.getUniqueId())) return CreateResult.ALREADY_IN_PARTY;
        PartySettings settings = new PartySettings(config.isDefaultFriendlyFire(), config.isDefaultPublic());
        Party party = new Party(UUID.randomUUID(), leader.getUniqueId(), settings);
        parties.put(party.getId(), party);
        playerParty.put(leader.getUniqueId(), party.getId());
        dataStore.save(party);
        createBossBarIfEnabled(party);
        Bukkit.getPluginManager().callEvent(new PartyCreateEvent(party));
        return CreateResult.CREATED;
    }

    public enum InviteResult {
        SENT, NOT_IN_PARTY, NOT_LEADER_OR_MEMBER, TARGET_OFFLINE, TARGET_IN_PARTY,
        SELF, FULL, COOLDOWN
    }

    public InviteResult invite(Player inviter, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) return InviteResult.TARGET_OFFLINE;
        if (target.getUniqueId().equals(inviter.getUniqueId())) return InviteResult.SELF;

        Party party = getParty(inviter.getUniqueId()).orElse(null);
        if (party == null) {
            create(inviter);
            party = getParty(inviter.getUniqueId()).orElseThrow();
        }
        if (isInParty(target.getUniqueId())) return InviteResult.TARGET_IN_PARTY;

        boolean isAdmin = inviter.hasPermission("party.admin");
        if (!isAdmin && party.size() >= config.getMaxPartySize()) return InviteResult.FULL;

        long now = System.currentTimeMillis();
        long cdMillis = config.getInviteCooldownSeconds() * 1000L;
        Long last = inviteCooldowns.get(inviter.getUniqueId());
        if (last != null && now - last < cdMillis) return InviteResult.COOLDOWN;
        inviteCooldowns.put(inviter.getUniqueId(), now);

        long expires = now + config.getInviteExpirySeconds() * 1000L;
        PartyInvite invite = new PartyInvite(party.getId(), inviter.getUniqueId(), target.getUniqueId(), expires);
        invites.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(inviter.getUniqueId(), invite);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Map<UUID, PartyInvite> map = invites.get(target.getUniqueId());
            if (map == null) return;
            PartyInvite cur = map.get(inviter.getUniqueId());
            if (cur != null && cur.isExpired()) {
                map.remove(inviter.getUniqueId());
                if (target.isOnline()) {
                    messages.send(target, "invite-expired",
                            Messages.placeholders("player", inviter.getName()));
                }
            }
        }, config.getInviteExpirySeconds() * 20L);

        return InviteResult.SENT;
    }

    public long getInviteCooldownRemaining(UUID inviter) {
        Long last = inviteCooldowns.get(inviter);
        if (last == null) return 0;
        long remaining = (config.getInviteCooldownSeconds() * 1000L) - (System.currentTimeMillis() - last);
        return Math.max(0, remaining / 1000L + (remaining % 1000 > 0 ? 1 : 0));
    }

    public enum JoinResult { JOINED, ALREADY_IN_PARTY, NO_INVITE, EXPIRED, FULL, NOT_FOUND }

    public JoinResult join(Player target, String inviterName) {
        if (isInParty(target.getUniqueId())) return JoinResult.ALREADY_IN_PARTY;
        Player inviter = Bukkit.getPlayerExact(inviterName);
        UUID inviterId = inviter != null ? inviter.getUniqueId() : null;

        Map<UUID, PartyInvite> targetInvites = invites.get(target.getUniqueId());
        if (targetInvites == null || targetInvites.isEmpty()) return JoinResult.NO_INVITE;

        PartyInvite invite;
        if (inviterId != null) {
            invite = targetInvites.get(inviterId);
            if (invite == null) return JoinResult.NO_INVITE;
        } else {
            invite = targetInvites.values().stream()
                    .filter(i -> !i.isExpired())
                    .reduce((a, b) -> b.expiresAtMillis() > a.expiresAtMillis() ? b : a)
                    .orElse(null);
            if (invite == null) return JoinResult.NO_INVITE;
        }

        if (invite.isExpired()) {
            targetInvites.remove(invite.inviter());
            return JoinResult.EXPIRED;
        }

        Party party = parties.get(invite.partyId());
        if (party == null) {
            targetInvites.remove(invite.inviter());
            return JoinResult.NOT_FOUND;
        }
        if (party.size() >= config.getMaxPartySize()) return JoinResult.FULL;

        targetInvites.remove(invite.inviter());
        party.addMember(target.getUniqueId());
        playerParty.put(target.getUniqueId(), party.getId());
        dataStore.save(party);
        refreshBossBar(party);
        Bukkit.getPluginManager().callEvent(new PartyJoinEvent(party, target.getUniqueId()));
        broadcast(party, "joined-party", Messages.placeholders("player", target.getName()));
        return JoinResult.JOINED;
    }

    public boolean leave(UUID playerId, PartyLeaveEvent.Reason reason) {
        Party party = getParty(playerId).orElse(null);
        if (party == null) return false;
        return removeFromParty(party, playerId, reason);
    }

    public boolean disband(UUID byPlayerId) {
        Party party = getParty(byPlayerId).orElse(null);
        if (party == null) return false;
        if (!party.isLeader(byPlayerId)) return false;
        disbandInternal(party);
        return true;
    }

    private void disbandInternal(Party party) {
        for (UUID m : party.getMembers().toArray(new UUID[0])) {
            playerParty.remove(m);
            Player p = Bukkit.getPlayer(m);
            if (p != null) messages.send(p, "party-disbanded");
            Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, m, PartyLeaveEvent.Reason.DISBAND));
        }
        BossBar bar = bossBars.remove(party.getId());
        if (bar != null) bar.removeAll();
        parties.remove(party.getId());
        dataStore.delete(party);
    }

    private boolean removeFromParty(Party party, UUID playerId, PartyLeaveEvent.Reason reason) {
        if (!party.removeMember(playerId)) return false;
        playerParty.remove(playerId);
        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, playerId, reason));

        if (party.size() == 0) {
            BossBar bar = bossBars.remove(party.getId());
            if (bar != null) bar.removeAll();
            parties.remove(party.getId());
            dataStore.delete(party);
            return true;
        }

        if (party.isLeader(playerId)) {
            UUID next = party.getMembers().iterator().next();
            party.setLeader(next);
            Player np = Bukkit.getPlayer(next);
            if (np != null) {
                messages.send(np, "ownership-transferred",
                        Messages.placeholders("player", np.getName()));
            }
        }

        broadcast(party, "left-party",
                Messages.placeholders("player", nameOf(playerId)));
        dataStore.save(party);
        refreshBossBar(party);
        return true;
    }

    public enum KickResult { KICKED, NOT_LEADER, NOT_IN_PARTY, NOT_A_MEMBER, CANNOT_KICK_SELF }

    public KickResult kick(Player leader, String targetName) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null) return KickResult.NOT_IN_PARTY;
        if (!party.isLeader(leader.getUniqueId())) return KickResult.NOT_LEADER;

        UUID targetId = resolveMemberId(party, targetName);
        if (targetId == null) return KickResult.NOT_A_MEMBER;
        if (targetId.equals(leader.getUniqueId())) return KickResult.CANNOT_KICK_SELF;

        Player target = Bukkit.getPlayer(targetId);
        removeFromParty(party, targetId, PartyLeaveEvent.Reason.KICK);
        if (target != null) messages.send(target, "kicked-from-party");
        broadcast(party, "member-kicked", Messages.placeholders("player", targetName));
        return KickResult.KICKED;
    }

    public enum TransferResult { TRANSFERRED, NOT_LEADER, NOT_IN_PARTY, TARGET_NOT_MEMBER }

    public TransferResult transfer(Player leader, String targetName) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null) return TransferResult.NOT_IN_PARTY;
        if (!party.isLeader(leader.getUniqueId())) return TransferResult.NOT_LEADER;
        UUID targetId = resolveMemberId(party, targetName);
        if (targetId == null) return TransferResult.TARGET_NOT_MEMBER;
        party.setLeader(targetId);
        dataStore.save(party);
        refreshBossBar(party);
        broadcast(party, "ownership-transferred", Messages.placeholders("player", nameOf(targetId)));
        return TransferResult.TRANSFERRED;
    }

    public boolean toggleFriendlyFire(Player leader) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null || !party.isLeader(leader.getUniqueId())) return false;
        boolean now = !party.getSettings().isFriendlyFire();
        party.getSettings().setFriendlyFire(now);
        dataStore.save(party);
        broadcast(party, now ? "friendly-fire-on" : "friendly-fire-off", null);
        return true;
    }

    public boolean togglePublic(Player leader) {
        Party party = getParty(leader.getUniqueId()).orElse(null);
        if (party == null || !party.isLeader(leader.getUniqueId())) return false;
        party.getSettings().setPublic(!party.getSettings().isPublic());
        dataStore.save(party);
        return true;
    }


    public boolean sendPartyChat(Player sender, String message) {
        Party party = getParty(sender.getUniqueId()).orElse(null);
        if (party == null) return false;
        Map<String, String> ph = Messages.placeholders("player", sender.getName(), "message", message);
        String formatted = messages.get("chat-format", ph);
        for (Player p : party.getOnlineMembers()) p.sendMessage(formatted);
        return true;
    }

    public void broadcast(Party party, String key, Map<String, String> placeholders) {
        for (Player p : party.getOnlineMembers()) messages.send(p, key, placeholders);
    }


    public void handleQuit(UUID playerId) {
    }

    public void handleJoinServer(UUID playerId) {
        getParty(playerId).ifPresent(this::refreshBossBar);
    }


    private void createBossBarIfEnabled(Party party) {
        if (!config.isBossBarEnabled()) return;
        BossBar bar = Bukkit.createBossBar(buildBossTitle(party), config.getBossBarColor(), config.getBossBarStyle());
        bar.setProgress(1.0);
        bossBars.put(party.getId(), bar);
        for (Player p : party.getOnlineMembers()) bar.addPlayer(p);
    }

    public void refreshBossBar(Party party) {
        if (!config.isBossBarEnabled()) return;
        BossBar bar = bossBars.computeIfAbsent(party.getId(),
                id -> Bukkit.createBossBar("", config.getBossBarColor(), config.getBossBarStyle()));
        bar.setTitle(buildBossTitle(party));
        bar.removeAll();
        for (Player p : party.getOnlineMembers()) bar.addPlayer(p);
    }

    private String buildBossTitle(Party party) {
        return messages.get("boss-bar-title", Messages.placeholders(
                "size", String.valueOf(party.size()),
                "max", String.valueOf(config.getMaxPartySize()),
                "leader", nameOf(party.getLeaderId())));
    }


    private UUID resolveMemberId(Party party, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null && party.contains(p.getUniqueId())) return p.getUniqueId();
        for (UUID id : party.getMembers()) {
            String n = nameOf(id);
            if (n.equalsIgnoreCase(name)) return id;
        }
        return null;
    }

    public String nameOf(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

    public List<String> getInviterNamesFor(UUID target) {
        Map<UUID, PartyInvite> map = invites.get(target);
        if (map == null) return List.of();
        return map.values().stream()
                .filter(i -> !i.isExpired())
                .map(i -> nameOf(i.inviter()))
                .toList();
    }

    public void pruneInvites() {
        for (Map<UUID, PartyInvite> m : invites.values()) {
            Iterator<PartyInvite> it = m.values().iterator();
            while (it.hasNext()) if (it.next().isExpired()) it.remove();
        }
    }

}
