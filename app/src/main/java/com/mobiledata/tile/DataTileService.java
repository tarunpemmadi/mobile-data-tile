package com.mobiledata.tile;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quick Settings tile showing mobile data usage for the selected window
 * (today, or the current billing cycle).
 */
public class DataTileService extends TileService {

    private static final String TAG = "DataTileService";

    private Handler mainHandler;
    private ExecutorService executor;
    private volatile boolean listening;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        listening = true;
        refreshTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        listening = false;
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pi = PendingIntent.getActivity(
                        this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pi);
            } else {
                //noinspection deprecation
                startActivityAndCollapse(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch settings from tile", e);
        }
    }

    private void refreshTile() {
        final Tile tile = getQsTile();
        if (tile == null) return;

        executor.execute(() -> {
            long usage = UsageRepository.queryMobileBytes(this);
            mainHandler.post(() -> {
                if (listening) {
                    updateTileUI(tile, usage);
                    tile.updateTile();
                }
            });
        });
    }

    private void updateTileUI(Tile tile, long usageBytes) {
        SharedPreferences prefs =
                getSharedPreferences(UsageRepository.PREFS_NAME, MODE_PRIVATE);
        String mode = prefs.getString(UsageRepository.KEY_MODE, UsageRepository.MODE_DAILY);

        if (usageBytes < 0) {
            // No usage access yet: keep the tile tappable so it opens setup.
            tile.setLabel(getString(R.string.tile_setup));
            tile.setState(Tile.STATE_INACTIVE);
            return;
        }

        String window = mode.equals(UsageRepository.MODE_CYCLE)
                ? getString(R.string.window_cycle)
                : getString(R.string.window_today);
        tile.setLabel(UsageRepository.formatBytes(usageBytes) + " \u00b7 " + window);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_data_tile));
        if (Build.VERSION.SDK_INT >= 30) {
            tile.setSubtitle(getString(R.string.tile_subtitle));
        }
        tile.setState(usageBytes > 0 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    /** Ask the system to re-bind the tile and call onStartListening(). */
    static void requestTileRefresh(Context context) {
        try {
            requestListeningState(context, new ComponentName(context, DataTileService.class));
        } catch (Exception e) {
            Log.e(TAG, "Failed to request tile refresh", e);
        }
    }
}
