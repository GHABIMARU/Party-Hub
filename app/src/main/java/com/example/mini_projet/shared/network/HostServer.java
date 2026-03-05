package com.example.mini_projet.shared.network;

import android.util.Log;

import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.model.Player;
import com.example.mini_projet.shared.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HostServer {

    private static final String TAG = "HostServer";

    private ServerSocket   serverSocket;
    private DatagramSocket udpSocket;
    private volatile boolean running = false;
    private final ExecutorService sender = Executors.newSingleThreadExecutor();

    private final List<PrintWriter>         clients   = new ArrayList<>();
    private final Map<PrintWriter, Integer> clientIds = new HashMap<>();
    private int nextClientId = 1;

    private final GameEngine engine = GameEngine.getInstance();

    public void startServer() {
        running = true;
        startUdpDiscovery();
        startTcpAcceptor();
    }

    private void startUdpDiscovery() {
        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(Utils.UDP_PORT);
                udpSocket.setBroadcast(true);
                byte[] buf = new byte[256];
                Log.d(TAG, "UDP discovery on port " + Utils.UDP_PORT);
                while (running) {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                    if (Utils.DISCOVER_MSG.equals(msg)) {
                        String myIp = Utils.getLocalIp();
                        if (myIp == null) continue;
                        String reply = Utils.DISCOVER_ACK + ":" + myIp;
                        byte[] rb = reply.getBytes();
                        udpSocket.send(new DatagramPacket(rb, rb.length,
                                pkt.getAddress(), pkt.getPort()));
                        Log.d(TAG, "UDP replied: " + reply);
                    }
                }
            } catch (Exception e) {
                if (running) Log.e(TAG, "UDP: " + e.getMessage());
            }
        }, "Host-UDP").start();
    }

    private void startTcpAcceptor() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(Utils.TCP_PORT);
                Log.d(TAG, "TCP on port " + Utils.TCP_PORT);
                while (running) {
                    Socket socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);
                    PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    synchronized (clients) { clients.add(out); }
                    Log.d(TAG, "Client connected: " + socket.getInetAddress());
                    listenToClient(in, out);
                }
            } catch (Exception e) {
                if (running) Log.e(TAG, "TCP: " + e.getMessage());
            }
        }, "Host-TCP").start();
    }

    private void listenToClient(BufferedReader in, PrintWriter out) {
        new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    Log.d(TAG, "From client: " + line);
                    Message msg = Message.fromWire(line);
                    if (msg != null) handleIncoming(msg, out);
                }
            } catch (Exception e) {
                if (running) Log.w(TAG, "Client disconnected: " + e.getMessage());
            } finally {
                synchronized (clients)   { clients.remove(out); }
                synchronized (clientIds) { clientIds.remove(out); }
            }
        }, "Host-Client").start();
    }

    private void handleIncoming(Message msg, PrintWriter clientOut) {
        switch (msg.getType()) {

            case "JOIN": {
                String name = msg.getPayload().trim();
                if (name.isEmpty()) return;
                engine.addPlayer(name);

                int assignedId;
                synchronized (clientIds) {
                    assignedId = nextClientId++;
                    clientIds.put(clientOut, assignedId);
                }
                Log.d(TAG, "JOIN: " + name + " -> id=" + assignedId);

                sendToOne(clientOut, new Message("YOUR_ID", null, String.valueOf(assignedId)));

                String list = buildPlayerListString();
                broadcast(new Message("PLAYER_LIST", null, list));
                engine.handleMessage(new Message("PLAYER_LIST", null, list));
                break;
            }

            case "VOTE": {
                try {
                    engine.addVote(Integer.parseInt(msg.getPayload().trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Bad VOTE: " + msg.getPayload());
                }
                break;
            }

            case "READY": {
                try {
                    engine.addReady(Integer.parseInt(msg.getSenderId().trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Bad READY: " + msg.getSenderId());
                }
                break;
            }
        }
    }

    public void sendStartGame() {
        String word = engine.getSecretWord();
        Map<PrintWriter, Integer> snapshot;
        synchronized (clientIds) {
            snapshot = new HashMap<>(clientIds);
        }
        Log.d(TAG, "sendStartGame: " + snapshot.size() + " clients");
        sender.execute(() -> {
            for (Map.Entry<PrintWriter, Integer> entry : snapshot.entrySet()) {
                int    id   = entry.getValue();
                String role = engine.isImpostor(id) ? "IMPOSTOR" : word;
                Log.d(TAG, "  START_GAME -> id=" + id + " role=" + role);
                writeToOne(entry.getKey(), new Message("START_GAME", null, role));
            }
        });
    }

    public void broadcast(Message message) {
        List<PrintWriter> snapshot;
        synchronized (clients) {
            snapshot = new ArrayList<>(clients);
        }
        sender.execute(() -> {
            for (PrintWriter out : snapshot) {
                writeToOne(out, message);
            }
        });
    }

    private void writeToOne(PrintWriter out, Message message) {
        try {
            String wire = message.toWire();
            Log.d(TAG, "Sending: " + wire.trim());
            out.print(wire);
            out.flush();
        } catch (Exception e) {
            Log.e(TAG, "writeToOne: " + e.getMessage());
        }
    }

    private void sendToOne(PrintWriter out, Message message) {
        sender.execute(() -> writeToOne(out, message));
    }

    public void stopServer() {
        running = false;
        sender.shutdownNow();
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        try { if (udpSocket   != null) udpSocket.close();   } catch (Exception ignored) {}
    }

    private String buildPlayerListString() {
        StringBuilder sb = new StringBuilder();
        for (Player p : engine.getPlayers())
            sb.append(p.getId()).append(":").append(p.getName()).append(";");
        return sb.toString();
    }
}