package com.example.mini_projet.spyfall.game;

import android.util.Log;

import com.example.mini_projet.R;
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

    private static volatile GameEngine instance;
    public static GameEngine getInstance() {
        if (instance == null) synchronized (GameEngine.class) {
            if (instance == null) instance = new GameEngine();
        }
        return instance;
    }
    private GameEngine() {}

    private final List<Player>          players                 = new ArrayList<>();
    private final List<Player>          originalPlayers         = new ArrayList<>();
    private final List<String>          eliminatedImpostorNames = new ArrayList<>();
    private final Map<Integer, Integer> votes                   = new HashMap<>();
    private final List<Integer>         readyPlayers            = new ArrayList<>();

    private GameState gameState   = GameState.LOBBY;
    private int       revealIndex = 0;
    private String    secretWord;
    private String    result;

    private final Set<Integer>    impostorIds    = new HashSet<>();
    private int                   impostorCount  = 1;
    private List<WordList.Theme>  selectedThemes = new ArrayList<>();

    public enum Mode { PASS_PLAY, HOST, CLIENT }

    private Mode             gameMode         = Mode.PASS_PLAY;
    private int              myPlayerId       = -1;
    private String           myRole;
    private HostServer       hostServer;
    private ClientConnection clientConnection;
    private MainActivity     activity;

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

    public void setSelectedThemes(List<WordList.Theme> themes) {
        this.selectedThemes = (themes != null) ? new ArrayList<>(themes) : new ArrayList<>();
    }

    public boolean isImpostor(int playerId) {
        return impostorIds.contains(playerId);
    }

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

    public void reset() {
        synchronized (players) { players.clear(); }
        synchronized (originalPlayers) { originalPlayers.clear(); }
        synchronized (votes) { votes.clear(); }
        synchronized (readyPlayers) { readyPlayers.clear(); }

        eliminatedImpostorNames.clear();
        impostorIds.clear();

        gameState = GameState.LOBBY;
        revealIndex = 0;
        secretWord = null;
        result = null;
        myRole = null;
        myPlayerId = -1;

        if (hostServer != null) {
            hostServer.stopServer();
            hostServer = null;
        }
        if (clientConnection != null) {
            clientConnection.disconnect();
            clientConnection = null;
        }
        gameMode = Mode.PASS_PLAY;
    }

    public void restartGame() {
        synchronized (players) {
            players.clear();
            synchronized (originalPlayers) {
                players.addAll(originalPlayers);
            }
        }
        synchronized (votes) { votes.clear(); }
        synchronized (readyPlayers) { readyPlayers.clear(); }
        eliminatedImpostorNames.clear();
        impostorIds.clear();

        gameState = GameState.LOBBY;
        revealIndex = 0;
        secretWord = null;
        result = null;

        if (gameMode == Mode.HOST && hostServer != null) {
            hostServer.broadcast(new Message("STATE_CHANGE", null, GameState.LOBBY.name()));
        }
    }

    public boolean startGame() {
        List<Player> snap;
        synchronized (players) { snap = new ArrayList<>(players); }
        if (snap.size() < MIN_PLAYERS) return false;

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

        secretWord = WordList.getRandom(activity, selectedThemes.isEmpty() ? null : selectedThemes);
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

    public void goToVoting()     { resetVotes(); gameState = GameState.VOTING; }
    public void goToDiscussion() { gameState = GameState.DISCUSSION; }
    public void goToResult()     { gameState = GameState.RESULT; }

    public void resetVotes() {
        synchronized (votes) { votes.clear(); }
    }

    public void addVote(int votedPlayerId) {
        synchronized (votes) {
            votes.merge(votedPlayerId, 1, Integer::sum);
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
        if (activity == null) return;
        Map<Integer, Integer> snap;
        synchronized (votes) { snap = new HashMap<>(votes); }

        if (snap.isEmpty()) {
            setResult(activity.getString(R.string.spy_result_no_votes,
                    buildImpostorNames(),
                    (impostorIds.size() == 1 ? activity.getString(R.string.verb_wins) : activity.getString(R.string.verb_win))));
            goToResult();
            return;
        }

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

        if (top.size() > 1) {
            List<String> tiedNames = new ArrayList<>();
            synchronized (players) {
                for (Player p : players) {
                    if (top.contains(p.getId())) tiedNames.add(p.getName());
                }
            }
            setResult(activity.getString(R.string.spy_result_tie,
                    buildNameList(tiedNames),
                    buildImpostorNames()));
            goToResult();
            return;
        }

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
                setResult(activity.getString(R.string.spy_result_impostor_win_generic,
                        buildImpostorNames(), activity.getString(R.string.verb_win)));
                goToResult();
                return;
            }

            boolean wasImpostor = isImpostor(eliminated.getId());
            players.remove(idx);
            impostorIds.remove(eliminated.getId());

            if (wasImpostor) {
                eliminatedImpostorNames.add(eliminated.getName());
                String verb = eliminatedImpostorNames.size() == 1
                        ? activity.getString(R.string.verb_was) : activity.getString(R.string.verb_were);
                String plural = eliminatedImpostorNames.size() == 1 ? "" : "s";
                setResult(activity.getString(R.string.spy_result_crew_win,
                        buildNameList(eliminatedImpostorNames), verb, plural));
            } else {
                setResult(activity.getString(R.string.spy_result_impostor_win_innocent,
                        eliminated.getName(), buildImpostorNames(), activity.getString(R.string.verb_win)));
            }
            goToResult();
        }
    }

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

            case "START":
                gameState = GameState.ROLE_REVEAL;
                if (activity != null) activity.navigateTo(GameState.ROLE_REVEAL);
                break;

            case "VOTE":
                addVote(Integer.parseInt(msg.getPayload()));
                break;

            case "READY":
                addReady(Integer.parseInt(msg.getSenderId().trim()));
                break;

            case "RESULT":
                result = msg.getPayload();
                gameState = GameState.RESULT;
                if (activity != null) activity.navigateTo(GameState.RESULT);
                break;

            case "STATE_CHANGE":
                GameState newState = GameState.valueOf(msg.getPayload());
                gameState = newState;
                if (activity != null) activity.navigateTo(newState);
                break;
        }
    }

    private String buildPlayerListString() {
        StringBuilder sb = new StringBuilder();
        synchronized (players) {
            for (Player p : players) {
                sb.append(p.getId()).append(":").append(p.getName()).append(";");
            }
        }
        return sb.toString();
    }

    private String buildImpostorNames() {
        List<String> names = new ArrayList<>();
        synchronized (originalPlayers) {
            for (Player p : originalPlayers) {
                if (impostorIds.contains(p.getId())) names.add(p.getName());
            }
        }
        return buildNameList(names);
    }

    private String buildNameList(List<String> names) {
        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            sb.append(names.get(i));
            if (i < names.size() - 2) sb.append(", ");
            else if (i == names.size() - 2) sb.append(" & ");
        }
        return sb.toString();
    }
}
