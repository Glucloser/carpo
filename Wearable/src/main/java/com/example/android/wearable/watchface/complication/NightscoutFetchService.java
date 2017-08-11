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
import android.util.Log;

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
    public final static String UPDATE_DELAY_KEY = "NightscoutFetchServicePrefsKeyUpdateDelay";

    private RequestQueue queue;
    private SharedPreferences preferences;
    protected boolean isStopped;

    public static void schedule(Context ctx) {
        JobScheduler scheduler = (JobScheduler)ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(
                2392,
                new ComponentName(ctx, NightscoutFetchService.class)
        );
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setMinimumLatency(1000 * 60 * 10).setOverrideDeadline(1000 * 60 * 15);
        builder.setBackoffCriteria(1000 * 60 * 10, JobInfo.BACKOFF_POLICY_LINEAR);
        scheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (this.queue == null) {
            this.queue = Volley.newRequestQueue(this);
            this.preferences = this.getSharedPreferences(
                    NightscoutFetchService.NIGHTSCOUT_FETCH_SERVICE_PREFERENCES_FILE_KEY, 0);
            this.isStopped = false;
        }
        new FetchTask(this).execute(jobParameters, this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        this.isStopped = true;
        return true;
    }

    private class FetchTask extends AsyncTask {
        private final NightscoutFetchService service;
        FetchTask(NightscoutFetchService service) {
            this.service = service;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            final Moshi moshi = new Moshi.Builder().build();
            final JobParameters jobParameters = (JobParameters)objects[0];
            final JobService jobService = (JobService)objects[1];

            // Instantiate the RequestQueue.
            String url = "http://glucloser.com:8081/api/v1/entries.json";

            Log.d("NSFS", "Fetching");

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("NSFS", "Fetched");
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
                            editor.putLong(NightscoutFetchService.UPDATE_DELAY_KEY, System.currentTimeMillis());
                            editor.apply();

                            jobService.jobFinished(jobParameters, false);

                            if (service.isStopped == false) {
                                NightscoutFetchService.schedule(service);
                            }
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
