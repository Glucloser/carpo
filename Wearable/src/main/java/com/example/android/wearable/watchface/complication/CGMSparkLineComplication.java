package com.example.android.wearable.watchface.complication;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nathan on 6/23/17.
 */

public class CGMSparkLineComplication extends ComplicationProviderService {
    @Override
    public void onComplicationUpdate(int complicationId, int datatype, ComplicationManager complicationManager) {
        SharedPreferences preferences =
        getSharedPreferences(
                NightscoutFetchService.NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY, 0);
        String[] recentSValues = preferences.getString(
                NightscoutFetchService.RECENT_VALUES_KEY,
                "").split(",");

        if (recentSValues.length == 0  || (recentSValues.length == 1 && recentSValues[0] == "")) {
            complicationManager.noUpdateRequired(complicationId);
            return;
        }

        List<Integer> recentValues = new ArrayList<Integer>();
        Integer min = 1000, max = 0;
        for (String svalue : recentSValues) {
            if (svalue.isEmpty()) {
                continue;
            }
            Integer value = Integer.valueOf(svalue);
            if (value == null) {
                continue;
            }
            recentValues.add(value);
            min = value < min ? value : min;
            max = value > max ? value : max;
        }

        Bitmap bmap = Bitmap.createBitmap(200, 100, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);

        Integer xStep = bmap.getWidth() / (recentValues.size() + 1);
        Integer x = xStep, prevX = 0;
        float yScale = (float)bmap.getHeight() / (float)(max - min);
        Integer y = 0, prevY = 0;

        Integer firstValue = recentValues.get(0), lastValue = recentValues.get(recentValues.size() - 1);
        canvas.drawText(String.valueOf(firstValue), xStep, (firstValue - min) * yScale, paint);
        canvas.drawText(String.valueOf(lastValue),
                recentValues.size() * xStep,
                (lastValue - min) * yScale,
                paint);

        for (Integer value : recentValues) {
            y = (int)((float)(value - min) * yScale);
            canvas.drawPoint(x, y, paint);
            if (prevX == 0) {
                prevX = x;
                prevY = y;
                continue;
            }

            canvas.drawLine(prevX, prevY, x, y, paint);
            prevX = x;
            x += xStep;
            prevY = y;
        }

        ComplicationData.Builder builder = new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE);
        builder.setSmallImage(Icon.createWithBitmap(bmap)).setImageStyle(ComplicationData.IMAGE_STYLE_PHOTO);

        complicationManager.updateComplicationData(complicationId, builder.build());
    }
}
