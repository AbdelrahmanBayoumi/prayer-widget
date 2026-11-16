package com.bayoumi.prayerwidget;

import com.batoulapps.adhan.Prayer;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PrayerScheduleTest {
    private static final Instant EVENING = Instant.parse("2026-11-13T20:00:00Z");

    @Test
    public void correctedFajrAppliesToTomorrowAndBothCountdownModes() {
        var unadjusted = new PrayerSchedule(City.ALEXANDRIA, EVENING, prayer -> 35, prayer -> 0);
        ToIntFunction<Prayer> adjustment = prayer -> prayer == Prayer.FAJR ? 5 : 0;
        Instant correctedFajr = unadjusted.displayedTime.plus(5, ChronoUnit.MINUTES);

        var evening = new PrayerSchedule(City.ALEXANDRIA, EVENING, prayer -> 35, adjustment);
        assertTrue(evening.displaysTomorrow());
        assertEquals(correctedFajr, evening.displayedTime);

        var before = new PrayerSchedule(City.ALEXANDRIA, correctedFajr.minusSeconds(1), prayer -> 35, adjustment);
        assertFalse(before.elapsed);
        assertEquals(Prayer.FAJR, before.displayedPrayer);
        assertEquals(correctedFajr, before.nextRefresh());

        var after = new PrayerSchedule(City.ALEXANDRIA, correctedFajr, prayer -> 35, adjustment);
        assertTrue(after.elapsed);
        assertEquals(correctedFajr, after.displayedTime);
        assertEquals(correctedFajr.plus(35, ChronoUnit.MINUTES), after.nextRefresh());
    }

    @Test
    public void adjustedIshaStaysSelectedUntilLocalMidnight() {
        ToIntFunction<Prayer> untilMidnight = prayer -> prayer == Prayer.ISHA ? CountdownSettings.UNTIL_MIDNIGHT : 35;
        ToIntFunction<Prayer> adjustment = prayer -> prayer == Prayer.ISHA ? -4 : 0;
        Instant midnight = LocalDate.of(2026, 11, 14).atStartOfDay(PrayerSchedule.EGYPT_TIME).toInstant();
        var before = new PrayerSchedule(City.ALEXANDRIA, midnight.minusSeconds(1), untilMidnight, adjustment);
        var after = new PrayerSchedule(City.ALEXANDRIA, midnight, untilMidnight, adjustment);

        assertEquals(Prayer.ISHA, before.displayedPrayer);
        assertTrue(before.elapsed);
        assertEquals(midnight, before.nextRefresh());
        assertEquals(Prayer.FAJR, after.displayedPrayer);
        assertFalse(after.elapsed);
    }

    @Test
    public void longestAllowedCorrectionsDoNotReorderAdjacentTimes() {
        Prayer[] prayers = {Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA};
        for (City city : City.values()) {
            for (int month = 1; month <= 12; month++) {
                Instant midday = LocalDate.of(2026, month, 15).atTime(12, 0)
                        .atZone(PrayerSchedule.EGYPT_TIME).toInstant();
                var day = new PrayerSchedule(city, midday, prayer -> 35, prayer -> 0);
                for (int i = 1; i < prayers.length; i++) {
                    long gap = ChronoUnit.MINUTES.between(day.timeFor(prayers[i - 1]), day.timeFor(prayers[i]));
                    assertTrue(city + " " + month + " " + prayers[i],
                            gap > 2L * CalendarAdjustments.MAX_PRAYER_MINUTES);
                }
            }
        }
    }
}
