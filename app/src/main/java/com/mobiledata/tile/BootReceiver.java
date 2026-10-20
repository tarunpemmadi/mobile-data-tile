package com.mobiledata.tile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-schedules the daily tile refresh after a device reboot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            TileRefreshScheduler.scheduleDailyRefresh(context);
            DataTileService.requestTileRefresh(context);
        }
    }
}
