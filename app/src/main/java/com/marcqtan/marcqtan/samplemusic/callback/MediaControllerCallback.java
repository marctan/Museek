package com.marcqtan.marcqtan.samplemusic.callback;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.marcqtan.marcqtan.samplemusic.viewmodel.PlaybackViewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

/**
 * Created by Marc Q. Tan on 28/05/2020.
 */
public class MediaControllerCallback extends MediaControllerCompat.Callback {

    private PlaybackViewModel playbackViewModel;
    private MutableLiveData<MediaMetadataCompat> metaDataChanged;
    private MutableLiveData<Boolean> isSessionReady;

    public MediaControllerCallback(ViewModelStoreOwner owner) {
        playbackViewModel = new ViewModelProvider(owner).get(PlaybackViewModel.class);
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }

        playbackViewModel.updatePlaybackState(state);
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        super.onMetadataChanged(metadata);
        playbackViewModel.updateMediaDescription(metadata);
        playbackViewModel.updateDuration(metadata);
        metaDataChanged.setValue(metadata);
    }

    @Override
    public void onSessionReady() {
        super.onSessionReady();
        isSessionReady.setValue(true);
    }

    public MutableLiveData<MediaMetadataCompat> getMetaData() {
        if (metaDataChanged == null) {
            metaDataChanged = new MutableLiveData<>();
        }
        return metaDataChanged;
    }

    public MutableLiveData<Boolean> getIsSessionReady() {
        if (isSessionReady == null) {
            isSessionReady = new MutableLiveData<>();
        }
        return isSessionReady;
    }
}
