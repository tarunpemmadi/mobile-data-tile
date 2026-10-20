package com.mobiledata.tile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Schedules a daily alarm just after midnight so the tile's collapsed value
 * stays current even when the shade is never opened.
 */
public final class TileRefreshScheduler {

    private static final int REQUEST_CODE = 1001;
    private static final int REFRESH_MINUTE_OF_DAY = 5; // 00:05 local time

    private TileRefreshScheduler() {}

    static void scheduleDailyRefresh(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = refreshPendingIntent(context);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, REFRESH_MINUTE_OF_DAY);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Inexact repeating: battery friendly and needs no special permission.
        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pi);
    }

    private static PendingIntent refreshPendingIntent(Context context) {
        Intent intent = new Intent(context, TileRefreshReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
