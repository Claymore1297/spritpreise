package org.woheller69.spritpreise.ui.viewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.woheller69.spritpreise.database.CityToWatch;
import org.woheller69.spritpreise.database.Station;
import org.woheller69.spritpreise.database.SQLiteHelper;
import org.woheller69.spritpreise.services.UpdateDataWorker;
import org.woheller69.spritpreise.ui.CityFragment;
import org.woheller69.spritpreise.ui.updater.IUpdateableCityUI;

import java.util.Collections;
import java.util.List;

import static org.woheller69.spritpreise.services.UpdateDataWorker.KEY_SKIP_UPDATE_INTERVAL;

public class CityPagerAdapter extends FragmentStateAdapter implements IUpdateableCityUI {

    private final SQLiteHelper database;

    private List<CityToWatch> cities;

    //Adapter for the Viewpager switching between different locations
    public CityPagerAdapter(Context context, @NonNull FragmentManager supportFragmentManager, @NonNull Lifecycle lifecycle) {
        super(supportFragmentManager,lifecycle);
        this.database = SQLiteHelper.getInstance(context);
        loadCities();
    }

    public void loadCities() {
        this.cities = database.getAllCitiesToWatch();
        Collections.sort(cities, (o1, o2) -> o1.getRank() - o2.getRank());
    }

    @NonNull
    @Override
    public CityFragment createFragment(int position) {
        Bundle args = new Bundle();
        args.putInt("city_id", cities.get(position).getCityId());

        return CityFragment.newInstance(args);
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }


    public CharSequence getPageTitle(int position) {
        return cities.get(position).getCityName();
    }

    public static void refreshSingleData(Context context, Boolean asap, int cityId) {

        Data data = new Data.Builder()
            .putInt(UpdateDataWorker.KEY_CITY_ID, cityId)
            .putBoolean(UpdateDataWorker.KEY_SKIP_UPDATE_INTERVAL, true)
            .build();

        OneTimeWorkRequest request =
            new OneTimeWorkRequest.Builder(UpdateDataWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(request);

    }


    @Override
    public void processUpdateStations(List<Station> stations, int cityID) {

    }

    public int getCityIDForPos(int pos) {
            CityToWatch city = cities.get(pos);
                 return city.getCityId();
    }

    public int getPosForCityID(int cityID) {
        for (int i = 0; i < cities.size(); i++) {
            CityToWatch city = cities.get(i);
            if (city.getCityId() == cityID) {
                return i;
            }
        }
        return -1;  // item not found
    }

}
