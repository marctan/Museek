package com.marcqtan.marcqtan.samplemusic;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Marc Q. Tan on 04/05/2020.
 */
public class TrackModel {
    private static List<TrackModel> trackModels;

    String description;
    String title;
    String duration;
    String stream_url;
    String id;
    String genre;
    String artwork_url;

    static List<TrackModel> getTrackModels() {
        return trackModels;
    }

    static void setTrackModels(List<TrackModel> trackModels){
        TrackModel.trackModels = trackModels;
    }
}
