package com.example.mini_projet.shared.network;

import android.util.Log;

import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ClientConnection {

    private static final String TAG             = "ClientConnection";
    private static final int    DISCOVER_TRIES  = 5;
    private static final int    DISCOVER_TIMEOUT = 3000;

    private PrintWriter      out;
    private Socket           socket;
    private volatile boolean connected = false;
    private final Object     writeLock = new Object();

    public void discoverAndConnect(Runnable onConnected, Runnable onFailed) {
        new Thread(() -> {
            String hostIp = discoverHost();
            if (hostIp == null) {
                Log.e(TAG, "Discovery failed after " + DISCOVER_TRIES + " attempts");
                if (onFailed != null) onFailed.run();
                return;
            }
            Log.d(TAG, "Host found at: " + hostIp);
            connectToHost(hostIp, onConnected, onFailed);
        }, "Client-Discover").start();
    }

    private String discoverHost() {
        try (DatagramSocket udp = new DatagramSocket()) {
            udp.setBroadcast(true);
            udp.setSoTimeout(DISCOVER_TIMEOUT);
            byte[] sendBuf = Utils.DISCOVER_MSG.getBytes();

            for (int i = 1; i <= DISCOVER_TRIES; i++) {
                Log.d(TAG, "Discovery attempt " + i + "/" + DISCOVER_TRIES);
                try {
                    // Broadcast to 255.255.255.255
                    udp.send(new DatagramPacket(sendBuf, sendBuf.length,
                            InetAddress.getByName("255.255.255.255"), Utils.UDP_PORT));

                    // Also broadcast to subnet (e.g. 192.168.43.255)
                    String localIp = Utils.getLocalIp();
                    if (localIp != null) {
                        String subnet = subnetBroadcast(localIp);
                        if (subnet != null) {
                            udp.send(new DatagramPacket(sendBuf, sendBuf.length,
                                    InetAddress.getByName(subnet), Utils.UDP_PORT));
                            Log.d(TAG, "Also sent to " + subnet);
                        }
                    }

                    // Wait for reply
                    byte[] recvBuf = new byte[256];
                    DatagramPacket reply = new DatagramPacket(recvBuf, recvBuf.length);
                    udp.receive(reply);
                    String response = new String(reply.getData(), 0, reply.getLength()).trim();
                    Log.d(TAG, "Discovery response: " + response);

                    if (response.startsWith(Utils.DISCOVER_ACK + ":")) {
                        return response.substring((Utils.DISCOVER_ACK + ":").length()).trim();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "No reply on attempt " + i);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "UDP socket error: " + e.getMessage());
        }
        return null;
    }

    private void connectToHost(String hostIp, Runnable onConnected, Runnable onFailed) {
        try {
            Log.d(TAG, "TCP connecting to " + hostIp + ":" + Utils.TCP_PORT);
            socket = new Socket(hostIp, Utils.TCP_PORT);
            socket.setTcpNoDelay(true);

            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            connected = true;
            Log.d(TAG, "TCP connected!");
            if (onConnected != null) onConnected.run();

            listen(in);
        } catch (Exception e) {
            Log.e(TAG, "TCP connect failed: " + e.getMessage());
            connected = false;
            if (onFailed != null) onFailed.run();
        }
    }

    private void listen(BufferedReader in) {
        new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    Log.d(TAG, "Received: " + line);
                    Message msg = Message.fromWire(line);
                    if (msg != null) {
                        GameEngine.getInstance().handleMessage(msg);
                    } else {
                        Log.w(TAG, "Malformed message: " + line);
                    }
                }
            } catch (Exception e) {
                if (connected) Log.e(TAG, "listen error: " + e.getMessage());
            }
        }, "Client-Listen").start();
    }

    public void send(Message message) {
        if (!connected || out == null) {
            Log.w(TAG, "send() called but not connected");
            return;
        }
        new Thread(() -> {
            synchronized (writeLock) {
                String wire = message.toWire();
                Log.d(TAG, "Sending: " + wire.trim());
                out.print(wire);
                out.flush();
            }
        }, "Client-Send").start();
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    public boolean isConnected() { return connected; }

    private String subnetBroadcast(String ip) {
        try {
            String[] p = ip.split("\\.");
            return p.length == 4 ? p[0]+"."+p[1]+"."+p[2]+".255" : null;
        } catch (Exception e) { return null; }
    }
}