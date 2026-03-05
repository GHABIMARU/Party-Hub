package com.example.mini_projet.shared.model;

public class Message {

    private final String type;
    private final String senderId;
    private final String payload;

    public Message(String type, String senderId, String payload) {
        this.type     = type     != null ? type     : "";
        this.senderId = senderId != null ? senderId : "";
        this.payload  = payload  != null ? payload  : "";
    }

    public String getType()     { return type; }
    public String getSenderId() { return senderId; }
    public String getPayload()  { return payload; }

    public String toWire() {
        return type + "§" + senderId + "§" + payload + "\n";
    }

    public static Message fromWire(String line) {
        if (line == null) return null;
        String[] parts = line.trim().split("§", -1);
        if (parts.length < 3) return null;
        return new Message(parts[0], parts[1], parts[2]);
    }
}