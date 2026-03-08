package com.example.mini_projet.spyfall.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.mini_projet.spyfall.game.GameEngine;
import java.util.ArrayList;
import java.util.List;
import com.example.mini_projet.shared.model.Player;

public class GameViewModel extends ViewModel {
    private final MutableLiveData<List<String>> players = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<String>> getPlayers() {
        return players;
    }

    public void addPlayer(String name) {
        List<String> current = players.getValue();
        if (current != null) {
            current.add(name);
            players.setValue(current);
            GameEngine.getInstance().addPlayer(name);
        }
    }

    public void removePlayer(int index) {
        List<String> current = players.getValue();
        if (current != null && index >= 0 && index < current.size()) {
            current.remove(index);
            players.setValue(current);
            GameEngine.getInstance().removePlayerByIndex(index);
        }
    }
    
    public void reset() {
        players.setValue(new ArrayList<>());
        GameEngine.getInstance().reset();
    }
}
