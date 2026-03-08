package com.example.mini_projet.spyfall.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;
import com.example.mini_projet.spyfall.viewmodel.GameViewModel;

import java.util.List;

public class LobbyFragment extends Fragment {

    private GameViewModel viewModel;
    private LinearLayout playersView;
    private EditText nameInput;
    private TextView tvPlayerCount;
    private View tvNoPlayers;
    
    private TextView modePassPlay, modeHost, modeJoin;
    
    private TextView tvImpostorCount;
    private View impostorSettingsLabel, impostorSettingsLayout;
    private int impostorCount = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_lobby, container, false);

        playersView = root.findViewById(R.id.players_view);
        nameInput = root.findViewById(R.id.name_input);
        tvPlayerCount = root.findViewById(R.id.tv_player_count);
        tvNoPlayers = root.findViewById(R.id.tv_no_players);
        Button addBtn = root.findViewById(R.id.add_player_btn);
        Button startBtn = root.findViewById(R.id.start_game_btn);
        
        modePassPlay = root.findViewById(R.id.mode_pass_play);
        modeHost = root.findViewById(R.id.mode_host);
        modeJoin = root.findViewById(R.id.mode_join);

        tvImpostorCount = root.findViewById(R.id.tv_impostor_count);
        impostorSettingsLabel = root.findViewById(R.id.impostor_settings_label);
        impostorSettingsLayout = root.findViewById(R.id.impostor_settings_layout);
        Button btnMinus = root.findViewById(R.id.btn_minus_impostor);
        Button btnPlus = root.findViewById(R.id.btn_plus_impostor);

        setupModePicker();
        setupImpostorControls(btnMinus, btnPlus);

        addBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                viewModel.addPlayer(name);
                nameInput.setText("");
                hideKeyboard();
            } else {
                Toast.makeText(getContext(), R.string.lobby_enter_name, Toast.LENGTH_SHORT).show();
            }
        });

        startBtn.setOnClickListener(v -> {
            List<String> players = viewModel.getPlayers().getValue();
            int pCount = (players != null) ? players.size() : 0;
            
            if (pCount < 3) {
                Toast.makeText(getContext(), getString(R.string.lobby_min_players_error, 3), Toast.LENGTH_SHORT).show();
                return;
            }

            int maxAllowed = Math.max(1, (pCount - 1) / 2);
            if (impostorCount > maxAllowed) {
                Toast.makeText(getContext(), "Too many impostors for this player count!", Toast.LENGTH_SHORT).show();
                return;
            }

            GameEngine.getInstance().setImpostorCount(impostorCount);
            ((MainActivity) requireActivity()).navigateTo(GameState.THEME_PICKER);
        });

        viewModel.getPlayers().observe(getViewLifecycleOwner(), this::updatePlayersList);

        return root;
    }

    private void setupImpostorControls(Button minus, Button plus) {
        minus.setOnClickListener(v -> {
            if (impostorCount > 1) {
                impostorCount--;
                tvImpostorCount.setText(String.valueOf(impostorCount));
            }
        });
        plus.setOnClickListener(v -> {
            List<String> players = viewModel.getPlayers().getValue();
            int pCount = (players != null) ? players.size() : 0;
            int maxAllowed = Math.max(1, (pCount - 1) / 2);
            
            if (impostorCount < maxAllowed) {
                impostorCount++;
                tvImpostorCount.setText(String.valueOf(impostorCount));
            } else {
                Toast.makeText(getContext(), "Need more players for more impostors!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupModePicker() {
        View.OnClickListener modeListener = v -> {
            modePassPlay.setSelected(v.getId() == R.id.mode_pass_play);
            modeHost.setSelected(v.getId() == R.id.mode_host);
            modeJoin.setSelected(v.getId() == R.id.mode_join);
            
            updateModeStyle(modePassPlay);
            updateModeStyle(modeHost);
            updateModeStyle(modeJoin);

            boolean isJoin = v.getId() == R.id.mode_join;
            if (impostorSettingsLabel != null) impostorSettingsLabel.setVisibility(isJoin ? View.GONE : View.VISIBLE);
            if (impostorSettingsLayout != null) impostorSettingsLayout.setVisibility(isJoin ? View.GONE : View.VISIBLE);
        };

        modePassPlay.setOnClickListener(modeListener);
        modeHost.setOnClickListener(modeListener);
        modeJoin.setOnClickListener(modeListener);

        // Default
        modePassPlay.performClick();
    }

    private void updateModeStyle(TextView tv) {
        if (tv.isSelected()) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold));
        } else {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    public void refreshPlayersList() {
        if (viewModel != null) {
            updatePlayersList(viewModel.getPlayers().getValue());
        }
    }

    private void updatePlayersList(List<String> players) {
        playersView.removeAllViews();
        int count = players != null ? players.size() : 0;
        tvPlayerCount.setText(String.valueOf(count));
        tvNoPlayers.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

        if (players != null) {
            for (int i = 0; i < players.size(); i++) {
                playersView.addView(createPlayerItem(players.get(i), i));
            }
        }
        
        // Auto-adjust impostor count if players were removed
        int maxAllowed = Math.max(1, (count - 1) / 2);
        if (impostorCount > maxAllowed) {
            impostorCount = maxAllowed;
            if (tvImpostorCount != null) tvImpostorCount.setText(String.valueOf(impostorCount));
        }
    }

    private View createPlayerItem(String name, int index) {
        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackgroundResource(R.drawable.bg_agent_item);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        params.setMargins(0, 0, 0, dp(12));
        item.setLayoutParams(params);
        item.setPadding(dp(16), 0, dp(16), 0);

        // Side indicator bar
        View bar = new View(getContext());
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(3), dp(32));
        bar.setLayoutParams(barParams);
        bar.setBackgroundColor(index % 2 == 0 ? 0xFF448AFF : 0xFF4CAF50); // Alternating colors
        item.addView(bar);

        // Avatar container
        FrameLayout avatarFrame = new FrameLayout(getContext());
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        frameParams.setMargins(dp(16), 0, dp(16), 0);
        avatarFrame.setLayoutParams(frameParams);
        avatarFrame.setBackgroundResource(R.drawable.bg_timer_ring);
        avatarFrame.setBackgroundTintList(ColorStateList.valueOf(0x208A9BC4));
        
        TextView emoji = new TextView(getContext());
        emoji.setText(index % 2 == 0 ? "🕵️" : "🎭");
        emoji.setTextSize(20);
        emoji.setGravity(Gravity.CENTER);
        avatarFrame.addView(emoji);
        item.addView(avatarFrame);

        // Text container
        LinearLayout textContainer = new LinearLayout(getContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView label = new TextView(getContext());
        label.setText("AGENT " + String.format("%02d", index + 1));
        label.setTextSize(10);
        label.setTextColor(0xFF448AFF);
        label.setTypeface(null, Typeface.BOLD);
        label.setLetterSpacing(0.2f);
        textContainer.addView(label);

        TextView nameTv = new TextView(getContext());
        nameTv.setText(name);
        nameTv.setTextSize(18);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTypeface(null, Typeface.BOLD);
        textContainer.addView(nameTv);
        
        item.addView(textContainer);

        // Remove button
        ImageView removeBtn = new ImageView(getContext());
        removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeBtn.setColorFilter(0x408A9BC4);
        removeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        removeBtn.setClickable(true);
        removeBtn.setFocusable(true);
        removeBtn.setOnClickListener(v -> viewModel.removePlayer(index));
        item.addView(removeBtn);

        return item;
    }

    private void hideKeyboard() {
        if (getView() != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
