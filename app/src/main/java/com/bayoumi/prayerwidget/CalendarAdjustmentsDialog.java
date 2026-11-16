package com.bayoumi.prayerwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import com.batoulapps.adhan.Prayer;

import java.util.Locale;
import java.util.function.IntConsumer;

final class CalendarAdjustmentsDialog {
    private enum Row {
        FAJR(Prayer.FAJR, R.string.fajr),
        SUNRISE(Prayer.SUNRISE, R.string.sunrise),
        DHUHR(Prayer.DHUHR, R.string.dhuhr),
        ASR(Prayer.ASR, R.string.asr),
        MAGHRIB(Prayer.MAGHRIB, R.string.maghrib),
        ISHA(Prayer.ISHA, R.string.isha),
        HIJRI(Prayer.NONE, R.string.adjust_hijri);

        final Prayer prayer;
        final int label;

        Row(Prayer prayer, int label) {
            this.prayer = prayer;
            this.label = label;
        }
    }

    private final Activity activity;
    private final CalendarAdjustments settings;
    private final Runnable onChange;

    CalendarAdjustmentsDialog(Activity activity, Runnable onChange) {
        this.activity = activity;
        this.onChange = onChange;
        settings = new CalendarAdjustments(activity);
    }

    void show() {
        Row[] rows = Row.values();
        String[] labels = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            Row row = rows[i];
            boolean hijri = row == Row.HIJRI;
            int adjustment = hijri ? settings.hijriDays() : settings.minutesFor(row.prayer);
            int unit = hijri ? R.string.adjust_days : R.string.adjust_minutes;
            String amount = activity.getString(unit, signedNumber(adjustment));
            labels[i] = activity.getString(R.string.duration_row, activity.getText(row.label), amount);
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.calendar_adjustments)
                .setItems(labels, (dialog, position) -> edit(rows[position]))
                .show();
    }

    private static String signedNumber(int adjustment) {
        if (adjustment == 0) {
            return "0";
        }
        // Isolate the sign and number so Arabic text cannot reverse their order.
        return "\u2066" + String.format(Locale.ROOT, "%+d", adjustment) + "\u2069";
    }

    private void edit(Row row) {
        if (row == Row.HIJRI) {
            editValue(row.label, settings.hijriDays(), CalendarAdjustments.MAX_HIJRI_DAYS,
                    settings::saveHijriDays);
        } else {
            editValue(row.label, settings.minutesFor(row.prayer), CalendarAdjustments.MAX_PRAYER_MINUTES,
                    minutes -> settings.saveMinutes(row.prayer, minutes));
        }
    }

    private void editValue(int title, int current, int limit, IntConsumer save) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMaxValue(limit * 2);
        picker.setFormatter(position -> signedNumber(position - limit));
        picker.setValue(current + limit);
        picker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        picker.setWrapSelectorWheel(false);

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(title == R.string.adjust_hijri
                        ? R.string.adjust_hijri_hint : R.string.adjust_prayer_hint)
                .setView(picker)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    save.accept(picker.getValue() - limit);
                    onChange.run();
                    show();
                })
                .show();
    }
}
