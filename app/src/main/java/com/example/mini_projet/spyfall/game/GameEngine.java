package com.example.mini_projet.spyfall.game;

import android.util.Log;

import com.example.mini_projet.spyfall.ui.MainActivity;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.model.Player;
import com.example.mini_projet.shared.network.ClientConnection;
import com.example.mini_projet.shared.network.HostServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameEngine {

    private static final String TAG = "GameEngine";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile GameEngine instance;
    public static GameEngine getInstance() {
        if (instance == null) synchronized (GameEngine.class) {
            if (instance == null) instance = new GameEngine();
        }
        return instance;
    }
    private GameEngine() {}

    // ── Game state ────────────────────────────────────────────────────────────
    private final List<Player>          players                 = new ArrayList<>();
    private final List<Player>          originalPlayers         = new ArrayList<>();
    private final List<String>          eliminatedImpostorNames = new ArrayList<>();
    private final Map<Integer, Integer> votes                   = new HashMap<>();
    private final List<Integer>         readyPlayers            = new ArrayList<>();

    private GameState gameState   = GameState.LOBBY;
    private int       revealIndex = 0;
    private String    secretWord;
    private String    result;

    private final Set<Integer> impostorIds  = new HashSet<>();
    private int                impostorCount = 1;

    // ── Mode / network ────────────────────────────────────────────────────────
    public enum Mode { PASS_PLAY, HOST, CLIENT }

    private Mode             gameMode         = Mode.PASS_PLAY;
    private int              myPlayerId       = -1;
    private String           myRole;
    private HostServer       hostServer;
    private ClientConnection clientConnection;
    private MainActivity     activity;

    // ── Accessors ─────────────────────────────────────────────────────────────
    public void             setActivity(MainActivity a)             { this.activity = a; }
    public void             setHostServer(HostServer h)             { this.hostServer = h; }
    public HostServer       getHostServer()                         { return hostServer; }
    public void             setClientConnection(ClientConnection c) { this.clientConnection = c; }
    public ClientConnection getClientConnection()                   { return clientConnection; }
    public int              getMyPlayerId()                         { return myPlayerId; }
    public void             setMyPlayerId(int id)                   { this.myPlayerId = id; }
    public String           getMyRole()                             { return myRole; }
    public Mode             getMode()                               { return gameMode; }
    public void             setMode(Mode m)                         { this.gameMode = m; }
    public GameState        getGameState()                          { return gameState; }
    public String           getResult()                             { return result; }
    public void             setResult(String r)                     { this.result = r; }
    public String           getSecretWord()                         { return secretWord; }

    public void setImpostorCount(int n) {
        this.impostorCount = Math.max(1, n);
    }

    public boolean isImpostor(int playerId) {
        return impostorIds.contains(playerId);
    }

    // ── Players ───────────────────────────────────────────────────────────────
    public void addPlayer(String name) {
        if (name == null || name.trim().isEmpty()) return;
        synchronized (players) {
            players.add(new Player(players.size(), name.trim()));
        }
    }

    public List<Player> getPlayers() {
        synchronized (players) { return new ArrayList<>(players); }
    }

    public static final int MIN_PLAYERS = 3;

    public void removePlayer(int playerId) {
        synchronized (players) {
            players.removeIf(p -> p.getId() == playerId);
        }
    }

    // ── Game start ────────────────────────────────────────────────────────────
    public boolean startGame() {
        List<Player> snap;
        synchronized (players) { snap = new ArrayList<>(players); }
        if (snap.size() < MIN_PLAYERS) return false;

        // Save full roster so Play Again can restore eliminated players
        synchronized (originalPlayers) {
            originalPlayers.clear();
            originalPlayers.addAll(snap);
        }
        eliminatedImpostorNames.clear();

        int maxImpostors    = Math.max(1, (snap.size() - 1) / 2);
        int actualImpostors = Math.min(impostorCount, maxImpostors);

        Random rand = new Random(System.nanoTime());
        List<Integer> ids = new ArrayList<>();
        for (Player p : snap) ids.add(p.getId());
        Collections.shuffle(ids, rand);
        impostorIds.clear();
        for (int i = 0; i < actualImpostors; i++) impostorIds.add(ids.get(i));

        secretWord = WordList.getRandom();
        Log.d(TAG, "startGame: word=" + secretWord
                + " impostors=" + impostorIds + " total=" + snap.size());

        if (gameMode == Mode.PASS_PLAY) {
            revealIndex = 0;
        } else {
            myRole = isImpostor(myPlayerId) ? "IMPOSTOR" : secretWord;
            Log.d(TAG, "Host myRole=" + myRole);
        }
        gameState = GameState.ROLE_REVEAL;
        return true;
    }

    // ── Pass & Play reveal ────────────────────────────────────────────────────
    public Player getCurrentRevealPlayer() {
        synchronized (players) {
            if (revealIndex < 0 || revealIndex >= players.size()) return null;
            return players.get(revealIndex);
        }
    }

    public void advanceReveal() {
        revealIndex++;
        synchronized (players) {
            if (revealIndex >= players.size()) gameState = GameState.DISCUSSION;
        }
    }

    // ── State transitions ─────────────────────────────────────────────────────
    public void goToVoting()     { resetVotes(); gameState = GameState.VOTING; }
    public void goToDiscussion() { gameState = GameState.DISCUSSION; }
    public void goToResult()     { gameState = GameState.RESULT; }

    // ── Votes ─────────────────────────────────────────────────────────────────
    public void resetVotes() {
        synchronized (votes) { votes.clear(); }
    }

    public void addVote(int votedPlayerId) {
        synchronized (votes) {
            Integer current = votes.get(votedPlayerId);
            votes.put(votedPlayerId, current != null ? current + 1 : 1);
        }
        if (gameMode == Mode.HOST) checkIfAllVoted();
    }

    private void checkIfAllVoted() {
        int total = 0;
        synchronized (votes) { for (int c : votes.values()) total += c; }
        int count;
        synchronized (players) { count = players.size(); }
        Log.d(TAG, "Votes: " + total + "/" + count);
        if (total >= count) {
            tallyVotes();
            if (hostServer != null) {
                if (gameState == GameState.RESULT) {
                    hostServer.broadcast(new Message("RESULT", null, result));
                } else {
                    hostServer.broadcast(new Message("PLAYER_LIST", null, buildPlayerListString()));
                }
                hostServer.broadcast(new Message("STATE_CHANGE", null, gameState.name()));
            }
            if (activity != null) activity.navigateTo(gameState);
        }
    }

    public void tallyVotes() {
        Map<Integer, Integer> snap;
        synchronized (votes) { snap = new HashMap<>(votes); }

        // No votes cast — impostors win by default
        if (snap.isEmpty()) {
            setResult("No votes cast — "
                    + buildImpostorNames()
                    + " win" + (impostorIds.size() == 1 ? "s" : "")
                    + " by default! 😈");
            goToResult();
            return;
        }

        // Find player ID(s) with the most votes
        int maxVotes = 0;
        List<Integer> top = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : snap.entrySet()) {
            if (e.getValue() > maxVotes) {
                maxVotes = e.getValue();
                top.clear();
                top.add(e.getKey());
            } else if (e.getValue() == maxVotes) {
                top.add(e.getKey());
            }
        }

        // Tie — nobody eliminated, impostors survive = impostors win
        if (top.size() > 1) {
            List<String> tiedNames = new ArrayList<>();
            synchronized (players) {
                for (Player p : players) {
                    if (top.contains(p.getId())) tiedNames.add(p.getName());
                }
            }
            setResult("TIE: " + buildNameList(tiedNames) + " — "
                    + buildImpostorNames() + " survive! Impostors win! 😈");
            goToResult();
            return;
        }

        // Clear winner — eliminate the voted player
        int eliminatedId = top.get(0);
        synchronized (players) {
            Player eliminated = null;
            int idx = -1;
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getId() == eliminatedId) {
                    eliminated = players.get(i);
                    idx = i;
                    break;
                }
            }
            if (eliminated == null) {
                // Shouldn't happen — treat as impostors winning
                setResult(buildImpostorNames() + " win! 😈");
                goToResult();
                return;
            }

            boolean wasImpostor = isImpostor(eliminated.getId());
            players.remove(idx);
            impostorIds.remove(eliminated.getId());

            if (wasImpostor) {
                // Crewmates picked correctly — impostors found
                eliminatedImpostorNames.add(eliminated.getName());
                String label = eliminatedImpostorNames.size() == 1
                        ? " was the impostor" : " were the impostors";
                setResult(buildNameList(eliminatedImpostorNames) + label + "! Crewmates win! 🎉");
            } else {
                // Crewmates picked wrong — impostors win
                setResult(eliminated.getName() + " was innocent. "
                        + buildImpostorNames() + " win! 😈");
            }
            goToResult();
        }
    }

    // ── Ready system ──────────────────────────────────────────────────────────
    public void addReady(int playerId) {
        synchronized (readyPlayers) {
            if (!readyPlayers.contains(playerId)) readyPlayers.add(playerId);
            int playerCount;
            synchronized (players) { playerCount = players.size(); }
            Log.d(TAG, "Ready: " + readyPlayers.size() + "/" + playerCount);
            if (readyPlayers.size() >= playerCount) {
                readyPlayers.clear();
                goToDiscussion();
                if (hostServer != null)
                    hostServer.broadcast(new Message("STATE_CHANGE", null, GameState.DISCUSSION.name()));
                if (activity != null)
                    activity.navigateTo(GameState.DISCUSSION);
            }
        }
    }

    // ── Message handler ───────────────────────────────────────────────────────
    public void handleMessage(Message msg) {
        if (msg == null) return;
        Log.d(TAG, "handleMessage: " + msg.getType() + " | " + msg.getPayload());

        switch (msg.getType()) {

            case "YOUR_ID":
                myPlayerId = Integer.parseInt(msg.getPayload());
                Log.d(TAG, "myPlayerId=" + myPlayerId);
                break;

            case "PLAYER_LIST":
                synchronized (players) {
                    players.clear();
                    for (String part : msg.getPayload().split(";")) {
                        if (part.isEmpty()) continue;
                        String[] kv = part.split(":");
                        if (kv.length < 2) continue;
                        players.add(new Player(Integer.parseInt(kv[0]), kv[1]));
                    }
                }
                if (activity != null)
                    activity.runOnUiThread(() -> activity.refreshLobbyIfVisible());
                break;

            case "START_GAME":
                myRole    = msg.getPayload();
                gameState = GameState.ROLE_REVEAL;
                Log.d(TAG, "START_GAME myRole=" + myRole);
                if (activity != null) activity.navigateTo(GameState.ROLE_REVEAL);
                break;

            case "STATE_CHANGE":
                gameState = GameState.valueOf(msg.getPayload());
                Log.d(TAG, "STATE_CHANGE → " + gameState);
                if (activity != null) activity.navigateTo(gameState);
                break;

            case "RESULT":
                setResult(msg.getPayload());
                break;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String buildNameList(List<String> names) {
        if (names.isEmpty()) return "The impostor";
        if (names.size() == 1) return names.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(i == names.size() - 1 ? " & " : ", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private String buildImpostorNames() {
        List<String> names = new ArrayList<>();
        synchronized (players) {
            for (Player p : players) {
                if (impostorIds.contains(p.getId())) names.add(p.getName());
            }
        }
        return buildNameList(names);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    public void reset() {
        synchronized (players)         { players.clear(); }
        synchronized (originalPlayers) { originalPlayers.clear(); }
        synchronized (votes)           { votes.clear(); }
        synchronized (readyPlayers)    { readyPlayers.clear(); }
        impostorIds.clear();
        eliminatedImpostorNames.clear();
        gameState     = GameState.LOBBY;
        revealIndex   = 0;
        secretWord    = null;
        result        = null;
        gameMode      = Mode.PASS_PLAY;
        myPlayerId    = -1;
        myRole        = null;
        impostorCount = 1;
        if (hostServer       != null) { hostServer.stopServer();      hostServer = null; }
        if (clientConnection != null) { clientConnection.disconnect(); clientConnection = null; }
    }

    public void restartGame() {
        // Restore the full original roster — brings back any eliminated players
        synchronized (players) {
            players.clear();
            synchronized (originalPlayers) {
                if (!originalPlayers.isEmpty()) players.addAll(originalPlayers);
            }
        }
        synchronized (votes)        { votes.clear(); }
        synchronized (readyPlayers) { readyPlayers.clear(); }
        impostorIds.clear();
        eliminatedImpostorNames.clear();
        gameState   = GameState.LOBBY;
        revealIndex = 0;
        secretWord  = null;
        result      = null;
        myRole      = null;
        if (hostServer       != null) { hostServer.stopServer();      hostServer = null; }
        if (clientConnection != null) { clientConnection.disconnect(); clientConnection = null; }
        gameMode   = Mode.PASS_PLAY;
        myPlayerId = -1;
    }

    private String buildPlayerListString() {
        StringBuilder sb = new StringBuilder();
        synchronized (players) {
            for (Player p : players)
                sb.append(p.getId()).append(":").append(p.getName()).append(";");
        }
        return sb.toString();
    }
}