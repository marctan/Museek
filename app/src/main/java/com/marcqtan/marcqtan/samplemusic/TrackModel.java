package com.marcqtan.marcqtan.samplemusic;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Marc Q. Tan on 04/05/2020.
 */
public class TrackModel {
    String description;
    String title;
    String duration;
    String stream_url;
    String id;
    String genre;
    String artwork_url;

    @Override
    public boolean equals(Object obj) {
        return !super.equals(obj);
    }

    public int hashCode() {
        return id.hashCode();
    }
}
