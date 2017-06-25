package com.example.android.wearable.watchface.complication;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;


/**
 * Created by nathan on 6/24/17.
 */

public class NightscoutFetchService extends JobService {
    static final String NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY =
            "com.example.android.wearable.watchface.NIGHTSCOUT_FETCH_SERVICE_PREFERENES_FILE_KEY";
    public final static String LATEST_VALUE_KEY = "NightscoutFetchServicePrefsKey";
    public final static String RECENT_VALUES_KEY = "NightscoutFetchServicePrefsKeyRecentValues";

    private RequestQueue queue;
    private SharedPreferences preferences;

    public static Context ctx;

    public NightscoutFetchService() {
        this.queue = Volley.newRequestQueue(ctx);
        this.preferences = ctx.getSharedPreferences(
                        NightscoutFetchService.NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY, 0);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        new FetchTask().execute(jobParameters, this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private class FetchTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            final Moshi moshi = new Moshi.Builder().build();
            final JobParameters jobParameters = (JobParameters)objects[0];
            final JobService jobService = (JobService)objects[1];

            // Instantiate the RequestQueue.
            String url = "http://glucloser.com:8081/api/v1/entries.json";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Type parseType =
                                    Types.newParameterizedType(List.class, NightscoutEntry.class);
                            JsonAdapter<List<NightscoutEntry>> nightscoutResponseJsonAdapter =
                                    moshi.adapter(parseType);
                            String latestValue;
                            StringBuffer recentValues = new StringBuffer();
                            try {
                                List<NightscoutEntry> nightscoutResponse =
                                        nightscoutResponseJsonAdapter.fromJson(response);
                                if (nightscoutResponse == null) {
                                    latestValue = "No Response";
                                }
                                else if (nightscoutResponse.isEmpty()) {
                                    latestValue = "No Entries";
                                }
                                else {
                                    NightscoutEntry firstEntry = nightscoutResponse.get(0);
                                    latestValue = String.valueOf(firstEntry.sgv);
                                    for (NightscoutEntry entry : nightscoutResponse) {
                                        recentValues.append(entry.sgv).append(",");
                                    }
                                }
                            }
                            catch (Exception ex) {
                                latestValue = ex.getLocalizedMessage();
                            }

                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(NightscoutFetchService.LATEST_VALUE_KEY, latestValue);
                            editor.putString(NightscoutFetchService.RECENT_VALUES_KEY, recentValues.toString());
                            editor.apply();

                            jobService.jobFinished(jobParameters, false);

                            JobScheduler scheduler = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);
                            JobInfo.Builder builder = new JobInfo.Builder(jobParameters.getJobId() == 0 ? 1 : 0, new ComponentName(ctx, NightscoutFetchService.class));
                            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                            builder.setMinimumLatency(1000 * 5 * 60).setOverrideDeadline(1000 * 10 * 60);
                //            builder.setPeriodic(1000 * 10 * 60, 1000 * 5 * 60);
                            scheduler.schedule(builder.build());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            jobService.jobFinished(jobParameters, true);
                        }
                    });
            queue.add(stringRequest);
            return this;
        }
    }

}
