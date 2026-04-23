package com.ranawise.data;

import com.ranawise.party.Party;

import java.util.Collection;


public interface PartyDataStore {
    void init();
    void close();

    Collection<Party> loadAll();

    void save(Party party);

    void delete(Party party);
}
