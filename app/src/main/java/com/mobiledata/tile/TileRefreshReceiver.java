package com.mobiledata.tile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fired by the daily alarm; asks the tile to re-read usage stats. */
public class TileRefreshReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DataTileService.requestTileRefresh(context);
    }
}
