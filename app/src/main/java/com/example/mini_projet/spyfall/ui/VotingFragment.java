package com.example.mini_projet.spyfall.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.shared.model.Message;
import com.example.mini_projet.shared.model.Player;

import java.util.ArrayList;
import java.util.List;

public class VotingFragment extends Fragment {

    private final GameEngine engine = GameEngine.getInstance();

    private List<Player> players;
    private int selectedListPos = -1;
    private int selectedPlayerId = -1;
    private int currentVoterPos  = 0;

    private ListView listPlayers;
    private Button   btnConfirm;
    private TextView tvTurn;

    public VotingFragment() { super(R.layout.fragment_voting); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        tvTurn      = view.findViewById(R.id.tv_turn);
        listPlayers = view.findViewById(R.id.list_players);
        btnConfirm  = view.findViewById(R.id.btn_confirm_vote);


        players = engine.getPlayers();

        List<String> names = new ArrayList<>();
        for (Player p : players) names.add("  " + p.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_activated_1,
                names) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                v.setBackgroundResource(R.drawable.bg_player_item);
                TextView tv = v.findViewById(android.R.id.text1);
                tv.setTextColor(0xFFF0F4FF);
                tv.setTextSize(16f);
                tv.setPadding(40, 32, 40, 32);
                return v;
            }
        };

        listPlayers.setAdapter(adapter);
        listPlayers.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listPlayers.setSelector(android.R.color.transparent);

        listPlayers.setOnItemClickListener((parent, v, position, id) -> {
            Player tapped = players.get(position);


            if (engine.getMode() != GameEngine.Mode.PASS_PLAY
                    && tapped.getId() == engine.getMyPlayerId()) {
                Toast.makeText(requireContext(),
                        "Can't vote for yourself", Toast.LENGTH_SHORT).show();
                listPlayers.setItemChecked(position, false);
                selectedListPos  = -1;
                selectedPlayerId = -1;
                return;
            }

            selectedListPos  = position;
            selectedPlayerId = tapped.getId();
        });

        engine.resetVotes();
        updateTurnLabel();

        btnConfirm.setOnClickListener(v -> {
            if (selectedPlayerId == -1) {
                Toast.makeText(requireContext(),
                        "Select a player first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (engine.getMode() == GameEngine.Mode.PASS_PLAY) {
                handlePassPlayVote();
            } else {
                handleNetworkVote();
            }
        });

        return view;
    }

    private void handlePassPlayVote() {
        Player currentVoter = players.get(currentVoterPos);

        if (selectedPlayerId == currentVoter.getId()) {
            Toast.makeText(requireContext(),
                    "Can't vote for yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        engine.addVote(selectedPlayerId);
        currentVoterPos++;
        selectedListPos  = -1;
        selectedPlayerId = -1;
        listPlayers.clearChoices();
        listPlayers.invalidateViews();

        if (currentVoterPos < players.size()) {
            updateTurnLabel();
        } else {
            engine.tallyVotes();
            ((MainActivity) requireActivity()).navigateTo(engine.getGameState());
        }
    }

    private void handleNetworkVote() {
        btnConfirm.setEnabled(false);
        btnConfirm.setText("VOTE SUBMITTED — WAITING...");

        if (engine.getMode() == GameEngine.Mode.CLIENT) {
            engine.getClientConnection().send(new Message(
                    "VOTE",
                    String.valueOf(engine.getMyPlayerId()),
                    String.valueOf(selectedPlayerId)));
        } else {
            engine.addVote(selectedPlayerId);
        }
    }

    private void updateTurnLabel() {
        if (engine.getMode() == GameEngine.Mode.PASS_PLAY
                && currentVoterPos < players.size()) {
            tvTurn.setText("🎯  " + players.get(currentVoterPos).getName() + "'s turn");
            tvTurn.setVisibility(View.VISIBLE);
        } else {
            tvTurn.setVisibility(View.GONE);
        }
    }
}