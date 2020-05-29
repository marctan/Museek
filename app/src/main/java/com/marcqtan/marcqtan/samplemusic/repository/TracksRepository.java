package com.marcqtan.marcqtan.samplemusic.repository;

import android.util.Log;

import com.marcqtan.marcqtan.samplemusic.service.SCApi;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackCollection;
import com.marcqtan.marcqtan.samplemusic.service.TrackService;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Marc Q. Tan on 27/05/2020.
 */
public class TracksRepository {

    private SCApi api;
    private TrackService service;
    private static final String NC_MUSIC_ID = "16069159";
    private static final String client_id = "AIBMBzom4aIwS64tzA3uvg";
    private int pageNumber = 0;

    private static TracksRepository instance;

    public static TracksRepository getInstance() {
        if (instance == null) {
            instance = new TracksRepository();
        }
        return instance;
    }

    private TracksRepository() {
        api = new SCApi();
        service = api.getService();
    }

    public Single<TrackCollection> fetchUserTracks() {
        return service.fetchUserTracks(NC_MUSIC_ID, client_id, 200, pageNumber * 200)
                .subscribeOn(Schedulers.io())
                .doOnSuccess(trackCollection -> pageNumber++)
                .doOnError(throwable -> {
                    Log.d("Error", "Error calling api");
                })//404 or 500
                .retry(5)//retry 5 times first before showing alert dialog
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void resetPagenumber() {
        pageNumber = 0;
    }
}
