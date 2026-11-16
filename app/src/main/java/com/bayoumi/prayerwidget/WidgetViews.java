package com.bayoumi.prayerwidget;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.batoulapps.adhan.Prayer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

final class WidgetViews {
    private static final ULocale ARABIC = new ULocale("ar_EG@calendar=gregorian;numbers=arab");
    private static final ULocale LATIN = new ULocale("ar_EG@calendar=gregorian;numbers=latn");
    private static final ULocale LATIN_HIJRI = new ULocale("ar_EG@calendar=islamic-umalqura;numbers=latn");

    private enum Slot {
        FAJR(Prayer.FAJR, R.id.slot_fajr, R.string.fajr, R.drawable.ic_fajr),
        SUNRISE(Prayer.SUNRISE, R.id.slot_sunrise, R.string.sunrise, R.drawable.ic_sunrise),
        DHUHR(Prayer.DHUHR, R.id.slot_dhuhr, R.string.dhuhr, R.drawable.ic_sun),
        ASR(Prayer.ASR, R.id.slot_asr, R.string.asr, R.drawable.ic_sun),
        MAGHRIB(Prayer.MAGHRIB, R.id.slot_maghrib, R.string.maghrib, R.drawable.ic_maghrib),
        ISHA(Prayer.ISHA, R.id.slot_isha, R.string.isha, R.drawable.ic_isha);

        final Prayer prayer;
        final int container;
        final int name;
        final int icon;

        Slot(Prayer prayer, int container, int name, int icon) {
            this.prayer = prayer;
            this.container = container;
            this.name = name;
            this.icon = icon;
        }

        int nameFor(PrayerSchedule schedule) {
            return prayer == Prayer.DHUHR && schedule.isFriday() ? R.string.friday : name;
        }
    }

    private WidgetViews() {}

    static RemoteViews create(Context context, PrayerSchedule schedule, int layout) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        boolean compact = layout == R.layout.prayer_widget_compact;
        setDates(views, schedule.date, new CalendarAdjustments(context).hijriDays(), compact);
        for (Slot slot : Slot.values()) {
            RemoteViews column = prayerColumn(context, schedule, slot, compact);
            views.removeAllViews(slot.container);
            views.addView(slot.container, column);
        }
        setCountdown(context, views, schedule);
        return views;
    }

    private static void setDates(RemoteViews views, LocalDate date, int hijriDays, boolean compact) {
        Date localDay = atNoon(date);
        Date hijriDay = atNoon(date.plusDays(hijriDays));
        if (!compact) {
            views.setTextViewText(R.id.day, formatter("EEEE", LATIN).format(localDay));
        }
        String hijri = formatter(compact ? "dd MMMM y" : "d MMMM y", LATIN_HIJRI).format(hijriDay);
        if (compact) {
            hijri = formatter("EEEE", LATIN).format(localDay) + " " + hijri;
        }
        views.setTextViewText(R.id.hijri_date, hijri);
        views.setTextViewText(R.id.gregorian_date,
                formatter("d MMMM y", LATIN).format(localDay));
    }

    private static RemoteViews prayerColumn(Context context, PrayerSchedule schedule, Slot slot,
                                            boolean compact) {
        boolean selected = slot.prayer == schedule.displayedPrayer;
        Instant time = selected ? schedule.displayedTime : schedule.timeFor(slot.prayer);
        RemoteViews column = new RemoteViews(context.getPackageName(),
                compact ? R.layout.prayer_column_compact : R.layout.prayer_column);
        if (!compact) {
            column.setImageViewResource(R.id.prayer_icon, slot.icon);
        }
        column.setTextViewText(R.id.prayer_name, context.getText(slot.nameFor(schedule)));
        ULocale digits = selected ? ARABIC : LATIN;
        column.setTextViewText(R.id.prayer_time, formatter("h:mm a", digits).format(Date.from(time)));
        if (selected) {
            column.setInt(R.id.prayer_column, "setBackgroundResource", R.drawable.next_prayer);
        }
        return column;
    }

    private static void setCountdown(Context context, RemoteViews views, PrayerSchedule schedule) {
        int prayerName = schedule.displaysTomorrow() ? R.string.tomorrow_fajr
                : Slot.valueOf(schedule.displayedPrayer.name()).nameFor(schedule);
        int title = schedule.elapsed ? R.string.elapsed_title : R.string.remaining_title;
        views.setTextViewText(R.id.countdown_title, context.getString(title, context.getText(prayerName)));
        long offset = schedule.displayedTime.toEpochMilli() - Instant.now().toEpochMilli();
        views.setChronometerCountDown(R.id.countdown, !schedule.elapsed);
        views.setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + offset,
                schedule.elapsed ? "+ %s" : "− %s", true);
    }

    private static Date atNoon(LocalDate date) {
        return Date.from(date.atTime(12, 0).atZone(PrayerSchedule.EGYPT_TIME).toInstant());
    }

    private static SimpleDateFormat formatter(String pattern, ULocale locale) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
        formatter.setTimeZone(TimeZone.getTimeZone(PrayerSchedule.EGYPT_TIME.getId()));
        return formatter;
    }
}
