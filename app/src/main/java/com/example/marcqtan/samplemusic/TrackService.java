package com.example.marcqtan.samplemusic;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Marc Q. Tan on 04/05/2020.
 */
public interface TrackService {
    @GET("/tracks/{id}")
    Single<TrackModel> fetchTrack(@Path("id") String id);

    @GET("/users/{id}/tracks")
    Single<List<TrackModel>> fetchUserTracks(@Path("id") String id);

}
