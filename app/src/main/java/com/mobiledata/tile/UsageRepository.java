package com.mobiledata.tile;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

/**
 * Shared helper that reads mobile (cellular) data usage from the OS
 * {@link NetworkStatsManager} and formats byte counts for display.
 */
public final class UsageRepository {

    private static final String TAG = "UsageRepository";

    static final String PREFS_NAME = "data_tile_prefs";
    static final String KEY_MODE = "mode";
    static final String KEY_CYCLE_START_DAY = "cycle_start_day";
    static final String MODE_DAILY = "daily";
    static final String MODE_CYCLE = "cycle";

    private UsageRepository() {}

    /** True if the user has granted this app Usage Access in Settings. */
    static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.w(TAG, "Usage access check failed", e);
            return false;
        }
    }

    /**
     * Start of the current counting window (midnight for daily mode, last
     * reset day for cycle mode). Clamps the day to the month length so a
     * "31st" cycle works in 30-day and February months.
     */
    static long windowStart(String mode, int cycleStartDay, long now) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (MODE_CYCLE.equals(mode)) {
            int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int day = Math.min(Math.max(cycleStartDay, 1), lastDay);
            cal.set(Calendar.DAY_OF_MONTH, day);
            if (cal.getTimeInMillis() > now) {
                // Reset day hasn't happened yet this month -> previous cycle.
                cal.add(Calendar.MONTH, -1);
                int prevLast = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                cal.set(Calendar.DAY_OF_MONTH, Math.min(cycleStartDay, prevLast));
            }
        }
        return cal.getTimeInMillis();
    }

    /**
     * Total mobile (cellular) bytes for the current window.
     *
     * @return byte count, or -1 if usage access is missing or the query failed.
     */
    static long queryMobileBytes(Context context) {
        if (!hasUsageAccess(context)) return -1;
        try {
            NetworkStatsManager nsm =
                    (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
            if (nsm == null) return -1;

            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String mode = prefs.getString(KEY_MODE, MODE_DAILY);
            int cycleDay = prefs.getInt(KEY_CYCLE_START_DAY, 1);

            long now = System.currentTimeMillis();
            long start = windowStart(mode, cycleDay, now);

            // Best-effort subscriber ID. It requires READ_PHONE_STATE on old
            // Android and carrier privileges on Android 10+, so a SecurityException
            // here is expected and harmless: null queries all subscriptions.
            String subscriberId = null;
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                try {
                    subscriberId = tm.getSubscriberId();
                } catch (Throwable ignored) {
                    subscriberId = null;
                }
            }

            NetworkStats.Bucket bucket = nsm.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE, subscriberId, start, now);
            if (bucket == null) return -1;

            long rx = Math.max(bucket.getRxBytes(), 0);
            long tx = Math.max(bucket.getTxBytes(), 0);
            return rx + tx;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to query mobile usage", t);
            return -1;
        }
    }

    /** Human-readable byte count, e.g. {@code 4.2 GB}. */
    static String formatBytes(long bytes) {
        if (bytes < 0) return "\u2014";
        if (bytes < 1024) return bytes + " B";
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int i = -1;
        do {
            value /= 1024.0;
            i++;
        } while (value >= 1024.0 && i < units.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", value, units[i]);
    }
}
