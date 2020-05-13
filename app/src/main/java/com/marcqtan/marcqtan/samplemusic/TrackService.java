package com.marcqtan.marcqtan.samplemusic;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by Marc Q. Tan on 04/05/2020.
 */
public interface TrackService {
    @GET("/tracks/{id}")
    Single<TrackModel> fetchTrack(@Path("id") String id);

    @GET("/users/{id}/tracks?linked_partitioning=1")
    Single<TrackCollection> fetchUserTracks(@Path("id") String id, @Query("client_id") String clientId,
                                            @Query("limit") int limit, @Query("offset") int offset);
}
