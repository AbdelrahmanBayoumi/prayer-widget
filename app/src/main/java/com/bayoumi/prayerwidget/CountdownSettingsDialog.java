package com.bayoumi.prayerwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.batoulapps.adhan.Prayer;

final class CountdownSettingsDialog {
    private static final Prayer[] PRAYERS = {
            Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA
    };
    private static final int[] NAMES = {
            R.string.fajr, R.string.sunrise, R.string.dhuhr, R.string.asr, R.string.maghrib, R.string.isha
    };

    private final Activity activity;
    private final CountdownSettings settings;
    private final Runnable onChange;

    CountdownSettingsDialog(Activity activity, Runnable onChange) {
        this.activity = activity;
        this.onChange = onChange;
        settings = new CountdownSettings(activity);
    }

    void show() {
        String[] rows = new String[PRAYERS.length];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = activity.getString(R.string.duration_row, activity.getText(NAMES[i]),
                    durationLabel(settings.minutesFor(PRAYERS[i])));
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.countdown_settings)
                .setItems(rows, (dialog, position) -> edit(PRAYERS[position], NAMES[position]))
                .show();
    }

    private void edit(Prayer prayer, int name) {
        if (prayer != Prayer.ISHA) {
            editMinutes(prayer, name);
            return;
        }
        CharSequence[] options = { activity.getText(R.string.until_midnight),
                activity.getText(R.string.custom_duration) };
        new AlertDialog.Builder(activity)
                .setTitle(name)
                .setItems(options, (dialog, position) -> {
                    if (position == 0) {
                        save(prayer, CountdownSettings.UNTIL_MIDNIGHT);
                    } else {
                        editMinutes(prayer, name);
                    }
                })
                .show();
    }

    private void editMinutes(Prayer prayer, int name) {
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.duration_hint);
        int saved = settings.minutesFor(prayer);
        input.setText(Integer.toString(saved < 0 ? CountdownSettings.DEFAULT_MINUTES : saved));
        input.setSelectAllOnFocus(true);
        FrameLayout container = new FrameLayout(activity);
        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(name)
                .setMessage(R.string.countdown_summary)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> saveMinutes(prayer, input, dialog));
    }

    private void saveMinutes(Prayer prayer, EditText input, AlertDialog dialog) {
        int minutes = parseMinutes(input.getText().toString().trim());
        if (minutes < 0 || minutes > CountdownSettings.MAX_MINUTES) {
            input.setError(activity.getString(R.string.invalid_duration, CountdownSettings.MAX_MINUTES));
            return;
        }
        dialog.dismiss();
        save(prayer, minutes);
    }

    private static int parseMinutes(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException invalidDuration) {
            return -1;
        }
    }

    private String durationLabel(int minutes) {
        return switch (minutes) {
            case CountdownSettings.UNTIL_MIDNIGHT -> activity.getString(R.string.until_midnight);
            case 0 -> activity.getString(R.string.duration_zero);
            default -> activity.getString(R.string.duration_value, minutes);
        };
    }

    private void save(Prayer prayer, int minutes) {
        settings.save(prayer, minutes);
        onChange.run();
        show();
    }
}
