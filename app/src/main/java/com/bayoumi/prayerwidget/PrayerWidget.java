package com.bayoumi.prayerwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.time.Duration;

public class PrayerWidget extends AppWidgetProvider {
    private static final String TAG = "PrayerWidget";
    private static final String REFRESH = "com.bayoumi.prayerwidget.REFRESH";
    private static final String FALLBACK_REFRESH = "com.bayoumi.prayerwidget.FALLBACK_REFRESH";
    private static final int PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (REFRESH.equals(action) || FALLBACK_REFRESH.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) {
            refresh(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        refresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        refresh(context);
    }

    static void refresh(Context context) {
        AppWidgetManager widgets = AppWidgetManager.getInstance(context);
        int[] classicIds = widgets.getAppWidgetIds(new ComponentName(context, PrayerWidget.class));
        int[] compactIds = widgets.getAppWidgetIds(new ComponentName(context, CompactPrayerWidget.class));
        if (classicIds.length == 0 && compactIds.length == 0) {
            AlarmManager alarms = context.getSystemService(AlarmManager.class);
            alarms.cancel(scheduledRefresh(context, REFRESH));
            alarms.cancel(scheduledRefresh(context, FALLBACK_REFRESH));
            return;
        }

        PrayerSchedule schedule = PrayerSchedule.current(context);
        updateWidgets(context, schedule, classicIds, R.layout.prayer_widget);
        updateWidgets(context, schedule, compactIds, R.layout.prayer_widget_compact);
        scheduleRefresh(context, schedule.nextRefresh().toEpochMilli());
    }

    private static void updateWidgets(Context context, PrayerSchedule schedule, int[] ids, int layout) {
        if (ids.length == 0) {
            return;
        }
        RemoteViews views = WidgetViews.create(context, schedule, layout);
        PendingIntent openApp = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PENDING_FLAGS);
        views.setOnClickPendingIntent(R.id.widget_root, openApp);
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views);
    }

    static boolean canScheduleExactUpdates(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private static void scheduleRefresh(Context context, long refreshAt) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent refresh = scheduledRefresh(context, REFRESH);
        PendingIntent fallback = scheduledRefresh(context, FALLBACK_REFRESH);
        long fallbackAt = refreshAt;
        if (canScheduleExactUpdates(context)) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, refreshAt, refresh);
                fallbackAt += Duration.ofMinutes(2).toMillis();
            } catch (SecurityException permissionChanged) {
                Log.w(TAG, "Exact update unavailable; scheduling a flexible update", permissionChanged);
                alarms.cancel(refresh);
            }
        } else {
            alarms.cancel(refresh);
        }

        // Keep a non-waking update pending if Android revokes exact alarm access.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, fallbackAt, fallback);
    }

    static PendingIntent refreshIntent(Context context) {
        return PendingIntent.getBroadcast(context, 1,
                new Intent(context, PrayerWidget.class).setAction(REFRESH), PENDING_FLAGS);
    }

    private static PendingIntent scheduledRefresh(Context context, String action) {
        return PendingIntent.getBroadcast(context, 0,
                new Intent(context, PrayerWidget.class).setAction(action), PENDING_FLAGS);
    }
}
