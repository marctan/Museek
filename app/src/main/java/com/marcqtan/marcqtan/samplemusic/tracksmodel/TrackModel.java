package com.marcqtan.marcqtan.samplemusic.tracksmodel;

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

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getId() {
        return id;
    }

    public String getArtwork_url() {
        return artwork_url;
    }

    public String getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getStream_url() {
        return stream_url;
    }
}
