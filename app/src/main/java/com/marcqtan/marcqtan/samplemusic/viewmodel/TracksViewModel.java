package com.marcqtan.marcqtan.samplemusic.viewmodel;

import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackCollection;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackModel;
import com.marcqtan.marcqtan.samplemusic.repository.TracksRepository;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Marc Q. Tan on 27/05/2020.
 */
public class TracksViewModel extends ViewModel {

    private TracksRepository repository;
    private MutableLiveData<TrackCollection> collection;
    private MutableLiveData<List<TrackModel>> tracks;
    private CompositeDisposable disposable = new CompositeDisposable();


    public TracksViewModel() {
        repository = TracksRepository.getInstance();
    }

    public void queryTracks() {
        disposable.add(repository.fetchUserTracks().subscribe(result -> {
            collection.setValue(result);
        }, throwable -> {
            TrackCollection tc = new TrackCollection();
            tc.setNext_href("error");
            collection.setValue(tc);
        }));
    }

    public LiveData<TrackCollection> getCollectionTracks() {
        if (collection == null) {
            collection = new MutableLiveData<>(); //always reset the value of collection
        }
        return collection;
    }

    public void updateTracks(List<TrackModel> trackModels) {
        tracks.setValue(trackModels);
    }

    public LiveData<List<TrackModel>> getLoadedTracks() {
        if (tracks == null) {
            tracks = new MutableLiveData<>();
        }
        return tracks;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.clear();
    }

}
