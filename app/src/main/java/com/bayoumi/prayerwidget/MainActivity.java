package com.bayoumi.prayerwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public final class MainActivity extends Activity {
    private static final String PREVIEW_STATE = "compact_preview";
    private FrameLayout preview;
    private Button cityButton;
    private boolean compactPreview;

    private final Runnable refreshPreview = this::showPreview;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);
        compactPreview = savedState != null && savedState.getBoolean(PREVIEW_STATE);
        applyScreenInsets(findViewById(R.id.screen));

        preview = findViewById(R.id.widget_preview);
        cityButton = findViewById(R.id.choose_city);
        cityButton.setOnClickListener(view -> chooseCity());
        findViewById(R.id.switch_preview).setOnClickListener(view -> {
            compactPreview = !compactPreview;
            showPreview();
        });
        findViewById(R.id.add_widget).setOnClickListener(view -> chooseWidget());
        findViewById(R.id.countdown_settings).setOnClickListener(view ->
                new CountdownSettingsDialog(this, this::refreshPrayerViews).show());
        findViewById(R.id.calendar_adjustments).setOnClickListener(view ->
                new CalendarAdjustmentsDialog(this, this::refreshPrayerViews).show());
        findViewById(R.id.refresh_widgets).setOnClickListener(view -> {
            refreshPrayerViews();
            showUpdateStatus();
            Toast.makeText(this, R.string.times_refreshed, Toast.LENGTH_SHORT).show();
        });
        bindUpdateSettings();
    }

    private void bindUpdateSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent exactUpdateSettings = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", getPackageName(), null));
            findViewById(R.id.open_exact_update_settings)
                    .setOnClickListener(view -> openSettings(exactUpdateSettings));
        }
        findViewById(R.id.open_battery_settings).setOnClickListener(view -> openSettings(
                new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PREVIEW_STATE, compactPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrayerViews();
        showUpdateStatus();
    }

    @Override
    protected void onPause() {
        preview.removeCallbacks(refreshPreview);
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    private void applyScreenInsets(View screen) {
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            }

            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
    }

    private void showPreview() {
        City city = City.selected(this);
        cityButton.setText(getString(R.string.city_value, city.label));
        PrayerSchedule schedule = PrayerSchedule.current(this);
        int layout = compactPreview ? R.layout.prayer_widget_compact : R.layout.prayer_widget;
        Button switchPreview = findViewById(R.id.switch_preview);
        switchPreview.setText(compactPreview ? R.string.preview_classic : R.string.preview_compact);
        RemoteViews views = WidgetViews.create(this, schedule, layout);
        preview.removeAllViews();
        preview.addView(views.apply(this, preview), FrameLayout.LayoutParams.MATCH_PARENT,
                Math.round(172 * getResources().getDisplayMetrics().density));
        preview.removeCallbacks(refreshPreview);
        long delay = schedule.nextRefresh().toEpochMilli() - System.currentTimeMillis();
        preview.postDelayed(refreshPreview, Math.max(1, delay));
    }

    private void showUpdateStatus() {
        boolean exactEnabled = PrayerWidget.canScheduleExactUpdates(this);
        TextView exactStatus = findViewById(R.id.exact_update_status);
        exactStatus.setText(exactEnabled ? R.string.exact_update_enabled : R.string.exact_update_disabled);
        findViewById(R.id.open_exact_update_settings).setVisibility(exactEnabled ? View.GONE : View.VISIBLE);

        boolean batteryExempt = getSystemService(PowerManager.class)
                .isIgnoringBatteryOptimizations(getPackageName());
        TextView batteryStatus = findViewById(R.id.battery_status);
        batteryStatus.setText(batteryExempt ? R.string.battery_unrestricted : R.string.battery_optimized);
        findViewById(R.id.open_battery_settings).setVisibility(batteryExempt ? View.GONE : View.VISIBLE);
    }

    private void openSettings(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unavailable) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseCity() {
        City[] cities = City.values();
        String[] labels = Arrays.stream(cities).map(city -> city.label).toArray(String[]::new);
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_city)
                .setSingleChoiceItems(labels, City.selected(this).ordinal(), (dialog, position) -> {
                    cities[position].save(this);
                    refreshPrayerViews();
                    dialog.dismiss();
                })
                .show();
    }

    private void refreshPrayerViews() {
        showPreview();
        PrayerWidget.refresh(this);
    }

    private void chooseWidget() {
        CharSequence[] names = {getText(R.string.widget_classic), getText(R.string.widget_compact)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_widget)
                .setItems(names, (dialog, position) ->
                        addWidget(position == 0 ? PrayerWidget.class : CompactPrayerWidget.class,
                                names[position]))
                .show();
    }

    private void addWidget(Class<? extends PrayerWidget> widget, CharSequence name) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (manager.isRequestPinAppWidgetSupported()
                && manager.requestPinAppWidget(new ComponentName(this, widget), null,
                        PrayerWidget.refreshIntent(this))) {
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.manual_add, name))
                .setPositiveButton(R.string.understood, null)
                .show();
    }
}
