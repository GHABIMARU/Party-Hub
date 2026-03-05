package com.example.mini_projet.spyfall.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;

public class ResultFragment extends Fragment {

    private final GameEngine engine = GameEngine.getInstance();

    public ResultFragment() { super(R.layout.fragment_result); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) return null;

        TextView tvResultEmoji = view.findViewById(R.id.tv_result_emoji);
        TextView tvResult      = view.findViewById(R.id.tv_result);
        Button   btnRestart    = view.findViewById(R.id.btn_restart);
        Button   btnMainMenu   = view.findViewById(R.id.btn_main_menu);

        String result = engine.getResult();
        if (result == null) result = "Game Over";

        if (result.startsWith("TIE:")) {
            tvResultEmoji.setText("🤝");
            tvResult.setTextColor(0xFFF0B429);
        } else if (result.contains("Crewmates")) {
            tvResultEmoji.setText("🎉");
            tvResult.setTextColor(0xFF38B2AC);
        } else if (result.contains("Impostor") || result.contains("win")) {
            tvResultEmoji.setText("😈");
            tvResult.setTextColor(0xFFE53E3E);
        } else {
            tvResultEmoji.setText("🤷");
            tvResult.setTextColor(0xFFF0F4FF);
        }
        tvResult.setText(result);


        btnRestart.setOnClickListener(v -> {
            engine.restartGame();
            ((MainActivity) requireActivity()).navigateTo(GameState.LOBBY);
        });


        btnMainMenu.setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToHome());

        return view;
    }
}