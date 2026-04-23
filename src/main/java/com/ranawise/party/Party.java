package com.ranawise.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


public final class Party {

    private final UUID id;
    private UUID leaderId;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final PartySettings settings;
    private final long createdAt;

    public Party(UUID id, UUID leaderId, PartySettings settings) {
        this.id = id;
        this.leaderId = leaderId;
        this.settings = settings;
        this.createdAt = System.currentTimeMillis();
        this.members.add(leaderId);
    }

    public Party(UUID id, UUID leaderId, java.util.Collection<UUID> members, PartySettings settings) {
        this.id = id;
        this.leaderId = leaderId;
        this.settings = settings;
        this.createdAt = System.currentTimeMillis();
        this.members.add(leaderId);
        if (members != null) this.members.addAll(members);
    }

    public UUID getId() { return id; }
    public UUID getLeaderId() { return leaderId; }
    public PartySettings getSettings() { return settings; }
    public long getCreatedAt() { return createdAt; }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    public int size() { return members.size(); }

    public boolean contains(UUID id) { return members.contains(id); }

    public boolean isLeader(UUID id) { return leaderId.equals(id); }

    boolean addMember(UUID id) { return members.add(id); }

    boolean removeMember(UUID id) { return members.remove(id); }

    void setLeader(UUID newLeader) { this.leaderId = newLeader; }

    public Set<Player> getOnlineMembers() {
        return members.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
