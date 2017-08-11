package com.example.android.wearable.watchface.complication;

import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

/**
 * Created by nathan on 6/23/17.
 */

public class CGMValueComplication extends ComplicationProviderService {

    @Override
    public void onComplicationUpdate(final int complicationId,
                                     final int dataType,
                                     final ComplicationManager complicationManager) {
        NightscoutFetchService.schedule(this);
        SharedPreferences preferences =
                getSharedPreferences(
                        NightscoutFetchService.NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY, 0);
        String latestValue = preferences.getString(
                        NightscoutFetchService.LATEST_VALUE_KEY,
                        "?");
        Long updateDelay = preferences.getLong(NightscoutFetchService.UPDATE_DELAY_KEY,
                System.currentTimeMillis());
        Long delay = System.currentTimeMillis() - updateDelay;

        final ComplicationData.Builder builder = new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE);
        builder.setShortText(ComplicationText.plainText(latestValue));
        builder.setMinValue(0).setMaxValue(delay * 1000 * 60);
        complicationManager.updateComplicationData(complicationId, builder.build());
    }
}
