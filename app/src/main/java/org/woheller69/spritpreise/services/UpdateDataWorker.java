package org.woheller69.spritpreise.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.woheller69.spritpreise.BuildConfig;
import org.woheller69.spritpreise.R;
import org.woheller69.spritpreise.activities.NavigationActivity;
import org.woheller69.spritpreise.api.IHttpRequestForStations;
import org.woheller69.spritpreise.api.tankerkoenig.TKHttpRequestForStations;
import org.woheller69.spritpreise.database.CityToWatch;
import org.woheller69.spritpreise.database.SQLiteHelper;
import org.woheller69.spritpreise.database.Station;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UpdateDataWorker extends Worker {

    public static final String KEY_CITY_ID = "cityId";
    public static final String KEY_SKIP_UPDATE_INTERVAL = "skipUpdateInterval";

    private static final long MIN_UPDATE_INTERVAL = 20;

    private final SQLiteHelper dbHelper;
    private final SharedPreferences prefManager;

    public UpdateDataWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        dbHelper = SQLiteHelper.getInstance(context);
        prefManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public Result doWork() {

        if (!isOnline(2000)) {
            Handler h = new Handler(getApplicationContext().getMainLooper());
            h.post(() -> {
                if (NavigationActivity.isVisible) {
                    Toast.makeText(
                            getApplicationContext(),
                            getApplicationContext().getString(R.string.error_no_internet),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            return Result.failure();
        }

        int cityId = getInputData().getInt(KEY_CITY_ID, -1);
        boolean skipUpdateInterval =
                getInputData().getBoolean(KEY_SKIP_UPDATE_INTERVAL, false);

        if (cityId < 0) return Result.failure();

        CityToWatch city = dbHelper.getCityToWatch(cityId);
        handleUpdateStations(cityId, city.getLatitude(), city.getLongitude(), skipUpdateInterval);

        return Result.success();
    }

    private void handleUpdateStations(int cityId, float lat, float lon, boolean skipUpdateInterval) {

        long timestamp = 0;
        long systemTime = System.currentTimeMillis() / 1000;
        long updateInterval =
                (long) (Float.parseFloat(
                        prefManager.getString("pref_updateInterval", "15")
                ) * 60);

        List<Station> stations = dbHelper.getStationsByCityId(cityId);
        if (!stations.isEmpty()) {
            timestamp = stations.get(0).getTimestamp();
        }

        if (skipUpdateInterval) {
            if ((timestamp + MIN_UPDATE_INTERVAL - systemTime) > 0) {
                skipUpdateInterval = false;
            }
        }

        if (skipUpdateInterval || timestamp + updateInterval - systemTime <= 0) {
            IHttpRequestForStations stationsRequest =
                    new TKHttpRequestForStations(getApplicationContext());
            stationsRequest.perform(lat, lon, cityId);
        }
    }

    private boolean isOnline(int timeOut) {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future =
                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            URL url = new URL(BuildConfig.BASE_URL);
                            return InetAddress.getByName(url.getHost());
                        } catch (IOException e) {
                            return null;
                        }
                    });
            inetAddress = future.get(timeOut, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException | TimeoutException | java.util.concurrent.ExecutionException e) {
        }
        return inetAddress != null && !inetAddress.toString().isEmpty();
    }
}

