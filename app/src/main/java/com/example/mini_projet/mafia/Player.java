package com.example.mini_projet.mafia;

import java.io.Serializable;

public class Player implements Serializable {

    public enum Role {
        MAFIA, DOCTOR, DETECTIVE, CIVILIAN
    }

    private int id;
    private String name;
    private Role role;
    private boolean isAlive;

    public Player(int id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.isAlive = true;
    }

    // Getters
    public int getId()       { return id; }
    public String getName()  { return name; }
    public Role getRole()    { return role; }
    public boolean isAlive() { return isAlive; }

    // Setters
    public void setAlive(boolean alive) { isAlive = alive; }
    public void setRole(Role role)      { this.role = role; }
}