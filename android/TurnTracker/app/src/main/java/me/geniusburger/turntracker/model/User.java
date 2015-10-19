package me.geniusburger.turntracker.model;

public class User {
    public long id;
    public String username;
    public String displayName;
    public User(long id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }
}
