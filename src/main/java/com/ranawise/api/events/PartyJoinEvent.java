package com.ranawise.api.events;

import com.ranawise.party.Party;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PartyJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final UUID playerId;

    public PartyJoinEvent(Party party, UUID playerId) {
        this.party = party;
        this.playerId = playerId;
    }

    public Party getParty() { return party; }
    public UUID getPlayerId() { return playerId; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
