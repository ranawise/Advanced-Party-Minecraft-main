package com.ranawise.party;

public final class PartySettings {

    private boolean friendlyFire;
    private boolean publicParty;

    public PartySettings(boolean friendlyFire, boolean publicParty) {
        this.friendlyFire = friendlyFire;
        this.publicParty = publicParty;
    }

    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }

    public boolean isPublic() { return publicParty; }
    public void setPublic(boolean publicParty) { this.publicParty = publicParty; }
}
