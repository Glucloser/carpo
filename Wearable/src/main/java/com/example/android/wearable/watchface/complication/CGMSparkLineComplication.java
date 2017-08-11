package com.example.android.wearable.watchface.complication;

import android.content.Context;
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
import android.util.Log;

import com.example.android.wearable.watchface.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nathan on 6/23/17.
 */

public class CGMSparkLineComplication extends ComplicationProviderService {
    public static String SPARKLINE_WIDTH = "sparkline_width";
    public static String SPARKLINE_HEIGHT= "sparkline_height";


    @Override
    public void onComplicationUpdate(int complicationId, int datatype, ComplicationManager complicationManager) {
        NightscoutFetchService.schedule(this);

        SharedPreferences preferences =
        getSharedPreferences(
                NightscoutFetchService.NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY, 0);

        SharedPreferences facePrefs =
                getSharedPreferences(getString(R.string.carpo_analog_preference_file_key),
                        Context.MODE_PRIVATE);

        String[] recentSValues = preferences.getString(
                NightscoutFetchService.RECENT_VALUES_KEY,
                "").split(",");

        if (recentSValues.length == 0  || (recentSValues.length == 1 && recentSValues[0] == "")) {
            complicationManager.noUpdateRequired(complicationId);
            return;
        }

        int width = facePrefs.getInt(SPARKLINE_WIDTH, 0);
        int height = facePrefs.getInt(SPARKLINE_HEIGHT, 0);
        Log.d("SPARKLINE", String.valueOf(width) + "x" + String.valueOf(height));

        if (width * height == 0) {
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

        Bitmap bmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);

        Integer xStep = bmap.getWidth() / (recentValues.size() + 1);
        Integer x = xStep, prevX = 0;
        float yScale = (float)((float)bmap.getHeight() * 0.9) / (float)(max - min);
        Integer y = 0, prevY = 0;

        canvas.drawLine(0, (float)(bmap.getHeight() * 0.75),
                bmap.getWidth(), (float)(bmap.getHeight() * 0.75),
                paint);
        canvas.drawText(String.valueOf(min + ((max - min) * 0.25)),
                xStep, (float)(bmap.getHeight() * 0.75), paint);

        canvas.drawLine(0, (float)(bmap.getHeight() * 0.25),
                bmap.getWidth(), (float)(bmap.getHeight() * 0.25),
                paint);
        canvas.drawText(String.valueOf(min + ((max - min) * 0.75)),
                xStep, (float)(bmap.getHeight() * 0.25), paint);

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
        builder.setSmallImage(Icon.createWithBitmap(bmap));

        complicationManager.updateComplicationData(complicationId, builder.build());
    }
}
