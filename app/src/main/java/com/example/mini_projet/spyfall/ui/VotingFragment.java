package com.example.mini_projet.spyfall.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
    private int selectedListPos  = -1;
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

        // ── Entrance ──────────────────────────────────────────────────────
        View header = view.findViewById(R.id.voting_header);
        header.setAlpha(0f);
        header.setTranslationY(-dp(20));
        header.animate().alpha(1f).translationY(0f)
                .setDuration(400).setInterpolator(new DecelerateInterpolator()).start();

        btnConfirm.setAlpha(0f);
        btnConfirm.setTranslationY(dp(20));
        btnConfirm.animate().alpha(1f).translationY(0f)
                .setStartDelay(300).setDuration(350)
                .setInterpolator(new DecelerateInterpolator()).start();

        // ── Player list ───────────────────────────────────────────────────
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
                v.setBackgroundResource(R.drawable.bg_vote_item);
                TextView tv = v.findViewById(android.R.id.text1);
                tv.setTextColor(0xFFF0F4FF);
                tv.setTextSize(16f);
                tv.setPadding(dp(16), dp(18), dp(16), dp(18));

                // Staggered entrance for each row
                v.setAlpha(0f);
                v.setTranslationX(-dp(30));
                v.animate().alpha(1f).translationX(0f)
                        .setStartDelay(200 + position * 60L)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(0.7f))
                        .start();
                return v;
            }
        };

        listPlayers.setAdapter(adapter);
        listPlayers.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listPlayers.setSelector(android.R.color.transparent);
        listPlayers.setDivider(null);
        listPlayers.setDividerHeight(dp(8));

        listPlayers.setOnItemClickListener((parent, v, position, id) -> {
            Player tapped = players.get(position);

            if (engine.getMode() != GameEngine.Mode.PASS_PLAY
                    && tapped.getId() == engine.getMyPlayerId()) {
                Toast.makeText(requireContext(), R.string.voting_no_self_vote, Toast.LENGTH_SHORT).show();
                listPlayers.setItemChecked(position, false);
                selectedListPos = -1;
                selectedPlayerId = -1;
                return;
            }

            selectedListPos  = position;
            selectedPlayerId = tapped.getId();

            // Animate the selected row
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction(() ->
                            v.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(150)
                                    .setInterpolator(new OvershootInterpolator(2f)).start()
                    ).start();
        });

        engine.resetVotes();
        updateTurnLabel();

        btnConfirm.setOnClickListener(v -> {
            if (selectedPlayerId == -1) {
                // Shake the button
                ObjectAnimator shake = ObjectAnimator.ofFloat(btnConfirm, "translationX",
                        0, -14, 14, -10, 10, -5, 5, 0);
                shake.setDuration(350);
                shake.start();
                Toast.makeText(requireContext(), R.string.voting_select_first, Toast.LENGTH_SHORT).show();
                return;
            }
            btnConfirm.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                    .withEndAction(() -> {
                        btnConfirm.animate().scaleX(1f).scaleY(1f).setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.5f)).start();
                        if (engine.getMode() == GameEngine.Mode.PASS_PLAY) {
                            handlePassPlayVote();
                        } else {
                            handleNetworkVote();
                        }
                    }).start();
        });

        return view;
    }

    private void handlePassPlayVote() {
        Player currentVoter = players.get(currentVoterPos);
        if (selectedPlayerId == currentVoter.getId()) {
            Toast.makeText(requireContext(), R.string.voting_no_self_vote, Toast.LENGTH_SHORT).show();
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
        btnConfirm.setText(R.string.voting_submitted_waiting);
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
            String name = players.get(currentVoterPos).getName();
            tvTurn.setText(getString(R.string.voting_turn_label, name));

            // Animate label change
            tvTurn.setAlpha(0f);
            tvTurn.setTranslationY(-dp(10));
            tvTurn.setVisibility(View.VISIBLE);
            tvTurn.animate().alpha(1f).translationY(0f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        } else {
            tvTurn.setVisibility(View.GONE);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}