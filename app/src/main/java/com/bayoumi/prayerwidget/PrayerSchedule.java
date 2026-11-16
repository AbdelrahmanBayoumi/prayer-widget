package com.bayoumi.prayerwidget;

import android.content.Context;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.function.ToIntFunction;

final class PrayerSchedule {
    static final ZoneId EGYPT_TIME = ZoneId.of("Africa/Cairo");
    private static final Prayer[] DAILY_ORDER = {
            Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA
    };

    final LocalDate date;
    final Prayer displayedPrayer;
    final Instant displayedTime;
    final boolean elapsed;
    private final PrayerTimes times;
    private final ToIntFunction<Prayer> adjustments;
    private final Instant refreshAt;

    static PrayerSchedule current(Context context) {
        return new PrayerSchedule(City.selected(context), Instant.now(),
                new CountdownSettings(context)::minutesFor, new CalendarAdjustments(context)::minutesFor);
    }

    PrayerSchedule(City city, Instant now, ToIntFunction<Prayer> elapsedMinutes,
                   ToIntFunction<Prayer> adjustments) {
        this.adjustments = adjustments;
        date = now.atZone(EGYPT_TIME).toLocalDate();
        times = calculate(city, date);
        Instant midnight = date.plusDays(1).atStartOfDay(EGYPT_TIME).toInstant();
        Prayer next = upcomingPrayer(now);
        Instant nextTime = next == Prayer.NONE
                ? adjustedTime(calculate(city, date.plusDays(1)), Prayer.FAJR) : timeFor(next);

        // Before Fajr, a custom Isha duration can still be running from yesterday.
        Prayer previous = previousPrayer(next);
        Instant previousTime = next == Prayer.FAJR
                ? adjustedTime(calculate(city, date.minusDays(1)), previous) : timeFor(previous);
        int minutes = elapsedMinutes.applyAsInt(previous);
        Instant elapsedEnds = elapsedEnd(previousTime, minutes);
        elapsed = !now.isBefore(previousTime) && now.isBefore(elapsedEnds);
        displayedPrayer = elapsed ? previous : next == Prayer.NONE ? Prayer.FAJR : next;
        displayedTime = elapsed ? previousTime : nextTime;

        Instant nextChange = elapsed && elapsedEnds.isBefore(nextTime) ? elapsedEnds : nextTime;
        refreshAt = nextChange.isBefore(midnight) ? nextChange : midnight;
    }

    boolean isFriday() {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    boolean displaysTomorrow() {
        return !elapsed && displayedPrayer == Prayer.FAJR
                && displayedTime.atZone(EGYPT_TIME).toLocalDate().isAfter(date);
    }

    Instant timeFor(Prayer prayer) {
        return adjustedTime(times, prayer);
    }

    Instant nextRefresh() {
        return refreshAt;
    }

    private Prayer upcomingPrayer(Instant now) {
        for (Prayer prayer : DAILY_ORDER) {
            if (timeFor(prayer).isAfter(now)) {
                return prayer;
            }
        }
        return Prayer.NONE;
    }

    private static Prayer previousPrayer(Prayer next) {
        for (int i = 1; i < DAILY_ORDER.length; i++) {
            if (DAILY_ORDER[i] == next) {
                return DAILY_ORDER[i - 1];
            }
        }
        return Prayer.ISHA;
    }

    private static Instant elapsedEnd(Instant prayerTime, int minutes) {
        if (minutes == CountdownSettings.UNTIL_MIDNIGHT) {
            return prayerTime.atZone(EGYPT_TIME).toLocalDate().plusDays(1)
                    .atStartOfDay(EGYPT_TIME).toInstant();
        }
        return prayerTime.plus(minutes, ChronoUnit.MINUTES);
    }

    private Instant adjustedTime(PrayerTimes schedule, Prayer prayer) {
        // Adhan 1.2.1 rounds seconds and retains the calculation's milliseconds.
        return schedule.timeForPrayer(prayer).toInstant().truncatedTo(ChronoUnit.MINUTES)
                .plus(adjustments.applyAsInt(prayer), ChronoUnit.MINUTES);
    }

    private static PrayerTimes calculate(City city, LocalDate date) {
        DateComponents day = new DateComponents(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        return new PrayerTimes(city.coordinates, day, CalculationMethod.EGYPTIAN.getParameters());
    }
}
