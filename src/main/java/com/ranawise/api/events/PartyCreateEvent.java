package com.ranawise.api.events;

import com.ranawise.party.Party;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PartyCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Party party;

    public PartyCreateEvent(Party party) {
        this.party = party;
    }

    public Party getParty() { return party; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
