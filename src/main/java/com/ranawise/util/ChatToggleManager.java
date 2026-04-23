package com.ranawise.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ChatToggleManager {

    private final Set<UUID> toggled = new HashSet<>();

    public boolean isToggled(UUID id) { return toggled.contains(id); }

    public boolean toggle(UUID id) {
        if (toggled.remove(id)) return false;
        toggled.add(id);
        return true;
    }

    public void clear(UUID id) { toggled.remove(id); }
}
