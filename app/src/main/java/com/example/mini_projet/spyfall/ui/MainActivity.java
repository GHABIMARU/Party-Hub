package com.example.mini_projet.spyfall.ui;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import com.example.mini_projet.HomeActivity;
import com.example.mini_projet.R;
import com.example.mini_projet.spyfall.game.GameEngine;
import com.example.mini_projet.spyfall.game.GameState;

public class MainActivity extends AppCompatActivity {

    private final GameEngine engine = GameEngine.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        engine.setActivity(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (engine.getGameState() == GameState.LOBBY) {
                    engine.reset();
                    finish();
                }
                // Block back press mid-game
            }
        });

        if (savedInstanceState == null) {
            navigateTo(GameState.LOBBY);
        }
    }

    public void navigateTo(GameState state) {
        runOnUiThread(() -> {
            Fragment fragment;
            switch (state) {
                case THEME_PICKER: fragment = new ThemePickerFragment(); break;
                case ROLE_REVEAL:  fragment = new RoleRevealFragment();  break;
                case DISCUSSION:   fragment = new DiscussionFragment();  break;
                case VOTING:       fragment = new VotingFragment();      break;
                case RESULT:       fragment = new ResultFragment();      break;
                default:           fragment = new LobbyFragment();       break;
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
        });
    }

    public void navigateToHome() {
        engine.reset();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void refreshLobbyIfVisible() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof LobbyFragment) ((LobbyFragment) f).refreshPlayersList();
    }

    public void refreshRoleRevealIfVisible() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof RoleRevealFragment) ((RoleRevealFragment) f).refreshRole();
    }

    @Override
    protected void onDestroy() {
        engine.setActivity(null);
        super.onDestroy();
    }
}