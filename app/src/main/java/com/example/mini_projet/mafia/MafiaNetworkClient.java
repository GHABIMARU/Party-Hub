package com.example.mini_projet.mafia;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class MafiaNetworkClient {

    private static final String TAG              = "MafiaNetworkClient";
    private static final int    DISCOVER_TRIES   = 5;
    private static final int    DISCOVER_TIMEOUT = 3000;

    private PrintWriter      out;
    private Socket           socket;
    private volatile boolean connected = false;
    private final Object     writeLock = new Object();

    // ── Callbacks to Join UI ──────────────────────────────────────────────────
    public interface ClientCallback {
        void onConnected();
        void onFailed();
        void onLobbyUpdate(List<Player> players);
        void onRoleAssigned(Player.Role role, List<Integer> mafiaTeamIds);
        void onGameStart(int round);
        void onNightResult(String text);
        void onStateChange(String state);
        void onEliminated(String playerName, String roleName);   // ← NEW
        void onGameOver(String title, String message);
    }

    private ClientCallback callback;
    private int myId = -1;

    public void setCallback(ClientCallback cb) { this.callback = cb; }
    public int  getMyId()                      { return myId; }

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // ── Discover host via UDP, then TCP connect ───────────────────────────────
    public void discoverAndConnect() {
        new Thread(() -> {
            String hostIp = discoverHost();
            if (hostIp == null) {
                Log.e(TAG, "Discovery failed");
                if (callback != null) uiHandler.post(() -> callback.onFailed());
                return;
            }
            connectToHost(hostIp);
        }, "Mafia-Discover").start();
    }

    private String discoverHost() {
        try (DatagramSocket udp = new DatagramSocket()) {
            udp.setBroadcast(true);
            udp.setSoTimeout(DISCOVER_TIMEOUT);
            byte[] msg = MafiaNetworkServer.DISCOVER_MSG.getBytes();

            for (int i = 1; i <= DISCOVER_TRIES; i++) {
                Log.d(TAG, "Discovery attempt " + i);
                try {
                    // Broadcast to 255.255.255.255
                    udp.send(new DatagramPacket(msg, msg.length,
                            InetAddress.getByName("255.255.255.255"),
                            MafiaNetworkServer.UDP_PORT));

                    // Also broadcast to subnet (e.g. 192.168.x.255)
                    String localIp = MafiaNetworkServer.getLocalIp();
                    if (localIp != null) {
                        String subnet = subnetBroadcast(localIp);
                        if (subnet != null)
                            udp.send(new DatagramPacket(msg, msg.length,
                                    InetAddress.getByName(subnet),
                                    MafiaNetworkServer.UDP_PORT));
                    }

                    byte[] buf = new byte[256];
                    DatagramPacket reply = new DatagramPacket(buf, buf.length);
                    udp.receive(reply);
                    String response = new String(reply.getData(), 0, reply.getLength()).trim();

                    if (response.startsWith(MafiaNetworkServer.DISCOVER_ACK + ":")) {
                        String ip = response.substring(
                                (MafiaNetworkServer.DISCOVER_ACK + ":").length()).trim();
                        Log.d(TAG, "Host found: " + ip);
                        return ip;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "No reply attempt " + i);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "UDP error: " + e.getMessage());
        }
        return null;
    }

    private void connectToHost(String hostIp) {
        try {
            socket = new Socket(hostIp, MafiaNetworkServer.TCP_PORT);
            socket.setTcpNoDelay(true);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            connected = true;
            Log.d(TAG, "TCP connected to " + hostIp);
            if (callback != null) uiHandler.post(() -> callback.onConnected());
            listenLoop(in);
        } catch (Exception e) {
            Log.e(TAG, "TCP connect failed: " + e.getMessage());
            connected = false;
            if (callback != null) uiHandler.post(() -> callback.onFailed());
        }
    }

    // ── Listen loop ───────────────────────────────────────────────────────────
    private void listenLoop(BufferedReader in) {
        new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (Exception e) {
                if (connected) Log.e(TAG, "Listen error: " + e.getMessage());
            }
        }, "Mafia-Client-Listen").start();
    }

    private void handleServerMessage(String line) {
        String[] parts = line.trim().split("§", -1);
        if (parts.length < 3) return;
        String type    = parts[0];
        String payload = parts[2];

        Log.d(TAG, "Received: " + type + " | " + payload);

        switch (type) {

            case "YOUR_ID": {
                try { myId = Integer.parseInt(payload.trim()); }
                catch (NumberFormatException ignored) {}
                break;
            }

            case "LOBBY": {
                // "id:name;id:name;..."
                List<Player> players = parseLobby(payload);
                if (callback != null)
                    uiHandler.post(() -> callback.onLobbyUpdate(players));
                break;
            }

            case "YOUR_ROLE": {
                // "MAFIA|2,5" or "DOCTOR" or "DETECTIVE" or "CIVILIAN"
                Player.Role role = parseRole(payload);
                List<Integer> mafiaIds = parseMafiaIds(payload);
                if (callback != null)
                    uiHandler.post(() -> callback.onRoleAssigned(role, mafiaIds));
                break;
            }

            case "START": {
                int round = 1;
                try { round = Integer.parseInt(payload.trim()); } catch (NumberFormatException ignored) {}
                int finalRound = round;
                if (callback != null)
                    uiHandler.post(() -> callback.onGameStart(finalRound));
                break;
            }

            case "NIGHT_RESULT": {
                if (callback != null)
                    uiHandler.post(() -> callback.onNightResult(payload));
                break;
            }

            case "ELIMINATED": {
                // "PlayerName:ROLE"
                String[] ep = payload.split(":", 2);
                String pName = ep.length > 0 ? ep[0] : "Someone";
                String rName = ep.length > 1 ? ep[1] : "";
                if (callback != null)
                    uiHandler.post(() -> callback.onEliminated(pName, rName));
                break;
            }

            case "STATE": {
                if (callback != null)
                    uiHandler.post(() -> callback.onStateChange(payload));
                break;
            }

            case "GAME_OVER": {
                // "title|message"
                String[] gp = payload.split("\\|", 2);
                String title   = gp.length > 0 ? gp[0] : "Game Over";
                String message = gp.length > 1 ? gp[1] : "";
                if (callback != null)
                    uiHandler.post(() -> callback.onGameOver(title, message));
                break;
            }
        }
    }

    // ── Send helpers ──────────────────────────────────────────────────────────
    public void sendJoin(String name) {
        send("JOIN", "", name);
    }

    public void send(String type, String senderId, String payload) {
        if (!connected || out == null) return;
        new Thread(() -> {
            synchronized (writeLock) {
                out.print(type + "§" + senderId + "§" + payload + "\n");
                out.flush();
            }
        }, "Mafia-Send").start();
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() { return connected; }

    // ── Parsing helpers ───────────────────────────────────────────────────────
    private List<Player> parseLobby(String payload) {
        // Format: "id:name:alive;id:name:alive;..."  (alive=1/0)
        List<Player> list = new ArrayList<>();
        for (String seg : payload.split(";")) {
            if (seg.isEmpty()) continue;
            String[] kv = seg.split(":", 3);
            if (kv.length < 2) continue;
            try {
                int    id    = Integer.parseInt(kv[0].trim());
                String name  = kv[1].trim();
                boolean alive = kv.length < 3 || "1".equals(kv[2].trim());
                Player p = new Player(id, name, Player.Role.CIVILIAN);
                p.setAlive(alive);
                list.add(p);
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    private Player.Role parseRole(String payload) {
        if (payload == null) return Player.Role.CIVILIAN;
        String roleName = payload.contains("|") ? payload.split("\\|")[0] : payload;
        try { return Player.Role.valueOf(roleName.trim()); }
        catch (IllegalArgumentException e) { return Player.Role.CIVILIAN; }
    }

    private List<Integer> parseMafiaIds(String payload) {
        List<Integer> ids = new ArrayList<>();
        if (payload == null || !payload.startsWith("MAFIA|")) return ids;
        String[] parts = payload.split("\\|");
        if (parts.length < 2) return ids;
        for (String s : parts[1].split(",")) {
            try { ids.add(Integer.parseInt(s.trim())); }
            catch (NumberFormatException ignored) {}
        }
        return ids;
    }

    private String subnetBroadcast(String ip) {
        try {
            String[] p = ip.split("\\.");
            return p.length == 4 ? p[0] + "." + p[1] + "." + p[2] + ".255" : null;
        } catch (Exception e) { return null; }
    }
}
