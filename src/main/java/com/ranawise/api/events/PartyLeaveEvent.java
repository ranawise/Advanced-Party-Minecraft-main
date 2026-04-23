package com.ranawise.api.events;

import com.ranawise.party.Party;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PartyLeaveEvent extends Event {

    public enum Reason { LEAVE, KICK, DISBAND }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;
    private final UUID playerId;
    private final Reason reason;

    public PartyLeaveEvent(Party party, UUID playerId, Reason reason) {
        this.party = party;
        this.playerId = playerId;
        this.reason = reason;
    }

    public Party getParty() { return party; }
    public UUID getPlayerId() { return playerId; }
    public Reason getReason() { return reason; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
