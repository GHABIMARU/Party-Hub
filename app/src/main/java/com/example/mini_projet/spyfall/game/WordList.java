package com.example.mini_projet.spyfall.game;

import android.content.Context;
import android.content.res.Resources;
import com.example.mini_projet.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Word bank organized by theme.
 * Fetches words from string-array resources to support localization.
 */
public final class WordList {

    private WordList() {}

    public enum Theme {
        OBJECTS   ("📦", R.string.theme_objects, R.array.theme_objects_words),
        PLACES    ("🗺️", R.string.theme_places,  R.array.theme_places_words),
        FOOD      ("🍕", R.string.theme_food,    R.array.theme_food_words),
        TECH      ("💻", R.string.theme_tech,    R.array.theme_tech_words),
        ANIMALS   ("🐾", R.string.theme_animals, R.array.theme_animals_words),
        JOBS      ("💼", R.string.theme_jobs,    R.array.theme_jobs_words),
        SPORTS    ("⚽", R.string.theme_sports,  R.array.theme_sports_words),
        SCHOOL    ("📚", R.string.theme_school,  R.array.theme_school_words),
        FUN       ("🎭", R.string.theme_fun,     R.array.theme_fun_words);

        public final String emoji;
        public final int labelRes;
        public final int arrayRes;
        Theme(String emoji, int labelRes, int arrayRes) {
            this.emoji = emoji;
            this.labelRes = labelRes;
            this.arrayRes = arrayRes;
        }
    }

    private static final Random RANDOM = new Random();

    public static String getRandom(Context context, List<Theme> selectedThemes) {
        Resources res = context.getResources();
        List<String> pool = new ArrayList<>();
        
        List<Theme> themes = (selectedThemes == null || selectedThemes.isEmpty())
                ? java.util.Arrays.asList(Theme.values()) : selectedThemes;
                
        for (Theme t : themes) {
            String[] words = res.getStringArray(t.arrayRes);
            for (String w : words) pool.add(w);
        }
        
        if (pool.isEmpty()) return "Mystery";
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    public static int getWordCountForTheme(Context context, Theme theme) {
        return context.getResources().getStringArray(theme.arrayRes).length;
    }

    public static int getTotalWordCount(Context context) {
        int count = 0;
        Resources res = context.getResources();
        for (Theme t : Theme.values()) {
            count += res.getStringArray(t.arrayRes).length;
        }
        return count;
    }
}