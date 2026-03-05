package com.example.mini_projet.mafia;

import java.util.ArrayList;
import java.util.List;

public class MafiaEventBus {

    public static final String EVENT_NIGHT_RESULT = "NIGHT_RESULT";
    public static final String EVENT_STATE        = "STATE";
    public static final String EVENT_ELIMINATED   = "ELIMINATED";
    public static final String EVENT_GAME_OVER    = "GAME_OVER";

    public interface Listener {
        void onEvent(String type, String payload);
    }

    private static final List<Listener> listeners = new ArrayList<>();

    public static void register(Listener l) {
        synchronized (listeners) { if (!listeners.contains(l)) listeners.add(l); }
    }

    public static void unregister(Listener l) {
        synchronized (listeners) { listeners.remove(l); }
    }

    public static void post(String type, String payload) {
        List<Listener> snap;
        synchronized (listeners) { snap = new ArrayList<>(listeners); }
        for (Listener l : snap) l.onEvent(type, payload);
    }
}
