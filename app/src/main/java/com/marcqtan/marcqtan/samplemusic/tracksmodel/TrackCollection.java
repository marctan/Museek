package com.marcqtan.marcqtan.samplemusic.tracksmodel;

import java.util.List;

/**
 * Created by Marc Q. Tan on 12/05/2020.
 */
public class TrackCollection {

    private List<TrackModel> collection;
    private String next_href;

    public List<TrackModel> getTracks() {
        return collection;
    }

    public void setNext_href(String next_href) {
        this.next_href = next_href;
    }

    public String getNext_href() {
        return next_href;
    }
}
