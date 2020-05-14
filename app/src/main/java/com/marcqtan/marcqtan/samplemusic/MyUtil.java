package com.marcqtan.marcqtan.samplemusic;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Marc Q. Tan on 15/05/2020.
 */
public final class MyUtil {
    public static void removeDuplicates(List<TrackModel> list)
    {
        Set<TrackModel> set = new LinkedHashSet<>(list);

        // Clear the list
        list.clear();

        // add the elements of set
        // with no duplicates to the list
        list.addAll(set);
    }
}
