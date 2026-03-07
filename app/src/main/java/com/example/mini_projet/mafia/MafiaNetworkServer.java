package com.example.mini_projet.mafia;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MafiaNetworkServer {

    private static final String TAG           = "MafiaNetworkServer";
    public  static final int    TCP_PORT      = 7654;
    public  static final int    UDP_PORT      = 7655;
    public  static final String DISCOVER_MSG  = "MAFIA_FIND";
    public  static final String DISCOVER_ACK  = "MAFIA_HERE";

    private ServerSocket     serverSocket;
    private DatagramSocket   udpSocket;
    private volatile boolean running = false;

    private final ExecutorService sender = Executors.newSingleThreadExecutor();

    private final List<ClientHandle>        handles    = new ArrayList<>();
    private final List<Player>              players    = new ArrayList<>();
    private final Map<PrintWriter, Integer> writerToId = new HashMap<>();

    private int nextId = 0;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    public interface HostCallback {
        void onPlayerJoined(List<Player> currentPlayers);
        void onError(String message);
    }

    /** Called on UI thread every time a client sends a VOTE message. */
    public interface VoteCallback {
        void onVoteReceived(int voterId, int targetId);
    }

    private HostCallback callback;
    private VoteCallback voteCallback;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public void setCallback(HostCallback cb)     { this.callback     = cb; }
    public void setVoteCallback(VoteCallback cb) { this.voteCallback = cb; }

    // ── Start / stop ──────────────────────────────────────────────────────────
    public void start() {
        running = true;
        startUdpDiscovery();
        startTcpAcceptor();
        Log.d(TAG, "Server started");
    }

    public void stop() {
        running = false;
        sender.shutdownNow();
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        try { if (udpSocket   != null) udpSocket.close();   } catch (Exception ignored) {}
    }

    // ── UDP discovery ─────────────────────────────────────────────────────────
    private void startUdpDiscovery() {
        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(UDP_PORT);
                udpSocket.setBroadcast(true);
                byte[] buf = new byte[256];
                while (running) {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                    if (DISCOVER_MSG.equals(msg)) {
                        String ip = getLocalIp();
                        if (ip == null) continue;
                        String reply = DISCOVER_ACK + ":" + ip;
                        byte[] rb = reply.getBytes();
                        udpSocket.send(new DatagramPacket(rb, rb.length,
                                pkt.getAddress(), pkt.getPort()));
                    }
                }
            } catch (Exception e) {
                if (running) Log.e(TAG, "UDP: " + e.getMessage());
            }
        }, "Mafia-Host-UDP").start();
    }

    // ── TCP acceptor ──────────────────────────────────────────────────────────
    private void startTcpAcceptor() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(TCP_PORT);
                Log.d(TAG, "TCP listening on " + TCP_PORT);
                while (running) {
                    Socket socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    ClientHandle handle = new ClientHandle(out, in);
                    synchronized (handles) { handles.add(handle); }
                    listenToClient(handle);
                }
            } catch (Exception e) {
                if (running) Log.e(TAG, "TCP accept: " + e.getMessage());
            }
        }, "Mafia-Host-TCP").start();
    }

    private void listenToClient(ClientHandle handle) {
        new Thread(() -> {
            try {
                String line;
                while (running && (line = handle.in.readLine()) != null) {
                    handleMessage(line, handle);
                }
            } catch (Exception e) {
                if (running) Log.w(TAG, "Client dropped: " + e.getMessage());
            } finally {
                synchronized (handles)    { handles.remove(handle); }
                synchronized (writerToId) { writerToId.remove(handle.out); }
            }
        }, "Mafia-Client").start();
    }

    // ── Incoming messages ─────────────────────────────────────────────────────
    private void handleMessage(String line, ClientHandle handle) {
        String[] parts = line.trim().split("§", -1);
        if (parts.length < 1) return;
        String type = parts[0];

        // ── VOTE: parts[1]=voterId  parts[2]=targetId ──────────────────────────
        if ("VOTE".equals(type) && parts.length >= 3) {
            try {
                int voterId  = parts[1].trim().isEmpty() ? -1
                        : Integer.parseInt(parts[1].trim());
                int targetId = Integer.parseInt(parts[2].trim());
                if (voteCallback != null)
                    uiHandler.post(() -> voteCallback.onVoteReceived(voterId, targetId));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Bad VOTE: " + line);
            }
            return;
        }

        // ── JOIN ───────────────────────────────────────────────────────────────
        if ("JOIN".equals(type) && parts.length >= 3) {
            String name = parts[2].trim();
            if (name.isEmpty()) return;

            Player p;
            synchronized (players) {
                p = new Player(nextId++, name, Player.Role.CIVILIAN);
                players.add(p);
            }
            synchronized (writerToId) { writerToId.put(handle.out, p.getId()); }
            Log.d(TAG, "JOIN: " + name + " id=" + p.getId());

            sendToOne(handle.out, "YOUR_ID", "", String.valueOf(p.getId()));
            broadcastLobby();

            if (callback != null) {
                List<Player> snap = new ArrayList<>(players);
                uiHandler.post(() -> callback.onPlayerJoined(snap));
            }
        }
    }

    // ── Host adds themselves (player 0, no socket) ────────────────────────────
    public void addHostPlayer(String name) {
        synchronized (players) {
            players.add(0, new Player(nextId++, name, Player.Role.CIVILIAN));
        }
        broadcastLobby();
    }

    // ── Assign roles + send START to all clients ──────────────────────────────
    public void startGame(int mafiaCount, boolean includeDoctor, boolean includeDetective) {
        List<Player> snap;
        synchronized (players) { snap = new ArrayList<>(players); }

        List<Player.Role> roles = new ArrayList<>();
        for (int i = 0; i < mafiaCount; i++) roles.add(Player.Role.MAFIA);
        if (includeDoctor)   roles.add(Player.Role.DOCTOR);
        if (includeDetective) roles.add(Player.Role.DETECTIVE);
        while (roles.size() < snap.size()) roles.add(Player.Role.CIVILIAN);
        Collections.shuffle(roles);

        synchronized (players) {
            for (int i = 0; i < players.size(); i++)
                players.get(i).setRole(roles.get(i));
        }

        List<Integer> mafiaIds = new ArrayList<>();
        for (Player p : snap) if (p.getRole() == Player.Role.MAFIA) mafiaIds.add(p.getId());

        List<ClientHandle> snapHandles;
        synchronized (handles) { snapHandles = new ArrayList<>(handles); }

        sender.execute(() -> {
            for (ClientHandle h : snapHandles) {
                Integer id;
                synchronized (writerToId) { id = writerToId.get(h.out); }
                if (id == null) continue;
                Player p = getPlayerById(id);
                if (p == null) continue;
                writeToOne(h.out, wire("YOUR_ROLE", "", buildRolePayload(p.getRole(), mafiaIds)));
                writeToOne(h.out, wire("START",     "", "1"));
            }
        });
    }

    // ── Broadcast methods — called by host game screens ───────────────────────

    /** Night result text → all clients show MafiaNetworkNightResultActivity */
    public void broadcastNightResult(String text) {
        broadcast("NIGHT_RESULT", "", text);
    }

    /**
     * State change → clients navigate screens.
     * state = "DAY"          → go to MafiaNetworkDayActivity
     * state = "ROLE_REVEAL"  → go to MafiaNetworkRoleRevealActivity (next night)
     */
    public void broadcastState(String state) {
        broadcast("STATE", "", state);
    }

    /** Elimination: "PlayerName:ROLE" → clients show who was eliminated */
    public void broadcastEliminated(String playerName, String roleName) {
        broadcast("ELIMINATED", "", playerName + ":" + roleName);
    }

    /** Updated alive player list after elimination */
    public void broadcastLobby() {
        broadcast("LOBBY", "", buildPlayerList());
    }

    /** Game over */
    public void broadcastGameOver(String title, String message) {
        broadcast("GAME_OVER", "", title + "|" + message);
    }

    // ── Internal broadcast / send ─────────────────────────────────────────────
    private void broadcast(String type, String senderId, String payload) {
        String wire = wire(type, senderId, payload);
        List<ClientHandle> snap;
        synchronized (handles) { snap = new ArrayList<>(handles); }
        sender.execute(() -> { for (ClientHandle h : snap) writeToOne(h.out, wire); });
    }

    private void sendToOne(PrintWriter out, String type, String senderId, String payload) {
        sender.execute(() -> writeToOne(out, wire(type, senderId, payload)));
    }

    private void writeToOne(PrintWriter out, String wire) {
        try { out.print(wire); out.flush(); }
        catch (Exception e) { Log.e(TAG, "write: " + e.getMessage()); }
    }

    private String wire(String type, String senderId, String payload) {
        return type + "§" + senderId + "§" + payload + "\n";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildPlayerList() {
        StringBuilder sb = new StringBuilder();
        synchronized (players) {
            for (Player p : players)
                sb.append(p.getId()).append(":").append(p.getName())
                        .append(":").append(p.isAlive() ? "1" : "0").append(";");
        }
        return sb.toString();
    }

    private String buildRolePayload(Player.Role role, List<Integer> mafiaIds) {
        if (role == Player.Role.MAFIA) {
            StringBuilder sb = new StringBuilder("MAFIA|");
            for (int i = 0; i < mafiaIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(mafiaIds.get(i));
            }
            return sb.toString();
        }
        return role.name();
    }

    private Player getPlayerById(int id) {
        synchronized (players) {
            for (Player p : players) if (p.getId() == id) return p;
        }
        return null;
    }

    public List<Player> getPlayers() {
        synchronized (players) { return new ArrayList<>(players); }
    }

    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) return ip;
                    }
                }
            }
            ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                        return addr.getHostAddress();
                }
            }
        } catch (Exception e) { Log.e(TAG, "getLocalIp: " + e.getMessage()); }
        return null;
    }

    private static class ClientHandle {
        final PrintWriter    out;
        final BufferedReader in;
        ClientHandle(PrintWriter out, BufferedReader in) { this.out = out; this.in = in; }
    }
}