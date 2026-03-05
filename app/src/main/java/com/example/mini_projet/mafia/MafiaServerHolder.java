package com.example.mini_projet.mafia;


public class MafiaServerHolder {

    private static MafiaNetworkServer server = null;

    public static void set(MafiaNetworkServer s)   { server = s; }
    public static MafiaNetworkServer get()          { return server; }
    public static boolean isHosting()               { return server != null; }
    public static void clear()                      { server = null; }
}
