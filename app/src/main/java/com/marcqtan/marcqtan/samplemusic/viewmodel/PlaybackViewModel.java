package com.marcqtan.marcqtan.samplemusic.viewmodel;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.marcqtan.marcqtan.samplemusic.utils.MyUtil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Created by Marc Q. Tan on 28/05/2020.
 */
public class PlaybackViewModel extends ViewModel {

    private MutableLiveData<Boolean> shouldShowProgressbar;
    private MutableLiveData<Boolean> shouldRunSeekbar;
    private MutableLiveData<PlaybackStateCompat> mLastPlaybackState;
    private MutableLiveData<Long> position;
    private MutableLiveData<Integer> duration;
    private MutableLiveData<MediaMetadataCompat> metadata;
    private MutableLiveData<MyUtil.REPEAT_MODE> rmode;
    private MutableLiveData<Boolean> enabled;

    public void updatePlaybackState(PlaybackStateCompat state) {
        if (state != null) {
            mLastPlaybackState.setValue(state);

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    shouldShowProgressbar.setValue(false);
                    shouldRunSeekbar.setValue(true);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                case PlaybackStateCompat.STATE_NONE:
                case PlaybackStateCompat.STATE_STOPPED:
                    shouldShowProgressbar.setValue(false);
                    shouldRunSeekbar.setValue(false);
                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    shouldShowProgressbar.setValue(true);
                    shouldRunSeekbar.setValue(false);
                    break;
                default:
                    shouldShowProgressbar.setValue(false);
                    break;
            }
        }
    }

    public void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getValue().getPosition();
        if (mLastPlaybackState.getValue().getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getValue().getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getValue().getPlaybackSpeed();
        }

        position.setValue(currentPosition);
    }

    public void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        duration.setValue((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
    }

    public MutableLiveData<Integer> getDuration() {
        if (duration == null) {
            duration = new MutableLiveData<>();
        }
        return duration;
    }

    public void updateMediaDescription(MediaMetadataCompat metadata) {
        MediaDescriptionCompat description = metadata.getDescription();
        if (description.getTitle() == null) {
            return;
        }
        this.metadata.setValue(metadata);
    }

    public LiveData<MediaMetadataCompat> getMetaData() {
        if (metadata == null) {
            metadata = new MutableLiveData<>();
        }
        return metadata;
    }

    public LiveData<PlaybackStateCompat> getmLastPlaybackState() {
        if (mLastPlaybackState == null) {
            mLastPlaybackState = new MutableLiveData<>();
        }
        return mLastPlaybackState;
    }

    public LiveData<Boolean> getShouldRunSeekbar() {
        if (shouldRunSeekbar == null) {
            shouldRunSeekbar = new MutableLiveData<>();
        }
        return shouldRunSeekbar;
    }

    public LiveData<Boolean> getShouldShowProgressbar() {
        if (shouldShowProgressbar == null) {
            shouldShowProgressbar = new MutableLiveData<>();
        }
        return shouldShowProgressbar;
    }

    public LiveData<Long> getPosition() {
        if (position == null) {
            position = new MutableLiveData<>();
        }
        return position;
    }

    public void repeatHandler(int value) {
        MyUtil.REPEAT_MODE rmode = MyUtil.REPEAT_MODE.values()[value];

        if (rmode.getValue() + 1 > 2) {
            this.rmode.setValue(MyUtil.REPEAT_MODE.NONE);
        } else {
            this.rmode.setValue(MyUtil.REPEAT_MODE.values()[rmode.getValue() + 1]);
        }
    }

    public LiveData<MyUtil.REPEAT_MODE> getRmode() {
        if (rmode == null) {
            rmode = new MutableLiveData<>();
        }
        return rmode;
    }

    public void shuffleHandler(MediaControllerCompat.TransportControls controls, MediaControllerCompat controllerCompat) {
        if (controllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            controls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
            enabled.setValue(false);
        } else {
            controls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            enabled.setValue(true);
        }
    }

    public MutableLiveData<Boolean> getEnabled() {
        if (enabled == null) {
            enabled = new MutableLiveData<>();
        }
        return enabled;
    }

    public void play(MediaControllerCompat mediaControllerCompat) {
        mediaControllerCompat.getTransportControls().play();
    }

    public void pause(MediaControllerCompat mediaControllerCompat) {
        mediaControllerCompat.getTransportControls().pause();
    }

    public void playFromMedia(MediaControllerCompat mediaControllerCompat, String mediaId, Bundle extras) {
        mediaControllerCompat.getTransportControls().playFromMediaId(mediaId, extras);
    }
}
