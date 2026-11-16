package com.bayoumi.prayerwidget;

import android.content.Context;
import android.content.SharedPreferences;

import com.batoulapps.adhan.Prayer;

final class CalendarAdjustments {
    // At most 30 minutes of relative adjustment keeps adjacent times ordered in the supported cities.
    static final int MAX_PRAYER_MINUTES = 15;
    static final int MAX_HIJRI_DAYS = 2;
    private static final String HIJRI_KEY = "hijri_adjustment";

    private final SharedPreferences preferences;

    CalendarAdjustments(Context context) {
        preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    int minutesFor(Prayer prayer) {
        return preferences.getInt(prayerKey(prayer), 0);
    }

    void saveMinutes(Prayer prayer, int minutes) {
        preferences.edit().putInt(prayerKey(prayer), minutes).apply();
    }

    int hijriDays() {
        return preferences.getInt(HIJRI_KEY, 0);
    }

    void saveHijriDays(int days) {
        preferences.edit().putInt(HIJRI_KEY, days).apply();
    }

    private static String prayerKey(Prayer prayer) {
        return "minutes_adjustment_" + prayer.name();
    }
}
