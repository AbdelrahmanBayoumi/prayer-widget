package com.bayoumi.prayerwidget;

import android.content.Context;
import android.content.SharedPreferences;

import com.batoulapps.adhan.Prayer;

final class CountdownSettings {
    static final int DEFAULT_MINUTES = 35;
    static final int MAX_MINUTES = 1440;
    static final int UNTIL_MIDNIGHT = -1;

    private final SharedPreferences preferences;

    CountdownSettings(Context context) {
        preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    int minutesFor(Prayer prayer) {
        return preferences.getInt("elapsed_" + prayer.name(), DEFAULT_MINUTES);
    }

    void save(Prayer prayer, int minutes) {
        preferences.edit().putInt("elapsed_" + prayer.name(), minutes).apply();
    }
}
