package com.ranawise.party;

import java.util.UUID;

public record PartyInvite(UUID partyId, UUID inviter, UUID target, long expiresAtMillis) {
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMillis;
    }
    public long secondsRemaining() {
        return Math.max(0, (expiresAtMillis - System.currentTimeMillis()) / 1000);
    }
}
