package com.flybbird.hometownweather.Fragment;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flybbird.hometownweather.Adapter.DBCityViewAdapter;
import com.flybbird.hometownweather.Data.CityListData;
import com.flybbird.hometownweather.R;

import java.util.List;

import co.moonmonkeylabs.realmsearchview.RealmSearchView;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;

/**
 * Created by SuyoungKang on 2016. 1. 24..
 */
public class SecondFragment extends Fragment {
    private Realm mRealm;
    private Thread mBackgroundThread;

    private RealmSearchView mRealmSearchView;
    private DBCityViewAdapter dbCityViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View secondView =  inflater.inflate(R.layout.fragment_second_view, container, false);
        mRealmSearchView = (RealmSearchView) secondView.findViewById(R.id.SEARCH_VIEW);


        return secondView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d("DEBUG", "* SecondFragment onStart");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("DEBUG", "* SecondFragment onResume");

        // Realm DB Init Instance.
        final RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(getActivity())
                                                        .name("country_code.realm")
                                                        .build();
        mRealm =  Realm.getInstance(realmConfiguration);
        mRealm.addChangeListener(realmListener);

        mBackgroundThread = new Thread(){
            @Override
            public void run() {
                // Realm instances cannot be shared between threads, so we need to create a new
                // instance on the background thread.
                Realm backgroundThreadRealm = Realm.getInstance(realmConfiguration);
                Log.d("DEBUG", "** Raw에서 DB로 읽어들인다 Start");
                loadCountryData(backgroundThreadRealm);
                Log.d("DEBUG", "** Raw에서 DB로 읽어들인다 End");
                backgroundThreadRealm.close();


                handler.sendEmptyMessage(0);
            }
        };


        if ( mRealm.isEmpty()) {
            // 비어있다면 다시 돌리도록 한다
            Log.d("DEBUG","스레드 시작");
            mBackgroundThread.start();
        }
        else {
            dbCityViewAdapter = new DBCityViewAdapter(getActivity(), mRealm, "name");
            // TODO: DB가 큰경우 viewAdapter에 thread나 분할로 데이터를 쌓을수 없는가요?
            mRealmSearchView.setAdapter(dbCityViewAdapter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("DEBUG", "* SecondFragment onStop");
    }


    @Override
    public void onPause() {
        super.onPause();

        Log.d("DEBUG", "* SecondFragment onPause");
        mRealm.removeChangeListener(realmListener);
        mRealm.close();
    }


    // Realm change listener that refreshes the UI when there is changes to Realm.
    private RealmChangeListener realmListener = new RealmChangeListener() {
        @Override
        public void onChange() {
           // dotsView.invalidate();
            Log.d("DEBUG", "** RealmChangeListener Event!");
        }
    };

    final Handler handler = new Handler() {
        public void handleMessage(Message msg)
        {
            dbCityViewAdapter = new DBCityViewAdapter(getActivity(), mRealm, "name");
            mRealmSearchView.setAdapter(dbCityViewAdapter);
        }

    };




    // Private method
    private void loadCountryData(Realm realm) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        try {
            JsonParser jsonParserCityList = jsonFactory.createParser(getResources().openRawResource(R.raw.city_list_simple));

            List<CityListData> entries =  objectMapper.readValue(jsonParserCityList, new TypeReference<List<CityListData>>() {});

            // TODO: entry에 대한 데이터를 전부다 넣을것이냐. 필터링 해서 넣을수 있는가.
//            while( entries.iterator().hasNext() ){
//                CityListData cityListData =  entries.iterator().next();
//                if ( !cityListData.getCountry().equalsIgnoreCase("KR") ){
//                    entries.remove(cityListData);
//                }
//            }
            realm.beginTransaction();
            realm.copyToRealm(entries);
            realm.commitTransaction();
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not load Ciy List data.");
        }
    }
}
