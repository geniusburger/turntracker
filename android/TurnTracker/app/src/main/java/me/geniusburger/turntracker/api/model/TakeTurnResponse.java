package me.geniusburger.turntracker.api.model;

import java.util.List;

import me.geniusburger.turntracker.model.Turn;
import me.geniusburger.turntracker.model.User;

public class TakeTurnResponse
{
    private long turnId;
    private List<User> users;
    private List<Turn> turns;

    public TakeTurnResponse(){}

    public long getTurnId() {
        return turnId;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Turn> getTurns() {
        return turns;
    }
}
