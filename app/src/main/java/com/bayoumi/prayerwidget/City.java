package com.bayoumi.prayerwidget;

import android.content.Context;

import com.batoulapps.adhan.Coordinates;

enum City {
    CAIRO("القاهرة", 30.0444, 31.2357),
    ALEXANDRIA("الإسكندرية", 31.2001, 29.9187),
    GIZA("الجيزة", 30.0131, 31.2089),
    MANSOURA("المنصورة", 31.0409, 31.3785),
    TANTA("طنطا", 30.7865, 31.0004),
    ZAGAZIG("الزقازيق", 30.5877, 31.5020),
    PORT_SAID("بورسعيد", 31.2653, 32.3019),
    SUEZ("السويس", 29.9668, 32.5498),
    ISMAILIA("الإسماعيلية", 30.5965, 32.2715),
    FAIYUM("الفيوم", 29.3084, 30.8428),
    MINYA("المنيا", 28.1099, 30.7503),
    ASYUT("أسيوط", 27.1809, 31.1837),
    LUXOR("الأقصر", 25.6872, 32.6396),
    ASWAN("أسوان", 24.0889, 32.8998),
    HURGHADA("الغردقة", 27.2579, 33.8116),
    SHARM_EL_SHEIKH("شرم الشيخ", 27.9158, 34.3300);

    private static final String PREFERENCES = "settings";
    private static final String CITY_KEY = "city";

    final String label;
    final Coordinates coordinates;

    City(String label, double latitude, double longitude) {
        this.label = label;
        this.coordinates = new Coordinates(latitude, longitude);
    }

    static City selected(Context context) {
        String saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(CITY_KEY, CAIRO.name());
        try {
            return valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            return CAIRO;
        }
    }

    void save(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(CITY_KEY, name()).apply();
    }
}
