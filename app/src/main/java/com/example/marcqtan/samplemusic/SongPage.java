package com.example.marcqtan.samplemusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class SongPage extends AppCompatActivity {

    TextView songName, subtitle, start, end;
    ImageView rewind, forward, back_btn, play_pause, album_artwork;
    SeekBar seekBar;
    private PlaybackStateCompat mLastPlaybackState;
    MediaControllerCallback mediaControllerCallback;
    Handler handler;
    SeekBarRunnable runnable;
    MediaSessionCompat.Token token;

    MediaControllerCompat mediaControllerCompat;

    int playState = 0;

    private class SeekBarRunnable implements Runnable {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    }

    private void initSeekBarRunnable(){
        handler = new Handler();
        runnable = new SeekBarRunnable();
    }

    private void initBroadCasters() {
        if (mediaControllerCompat == null && token != null) {
            try {
                mediaControllerCompat = new MediaControllerCompat(SongPage.this, token);
                updatePlaybackState(mediaControllerCompat.getPlaybackState());
                updateDuration(mediaControllerCompat.getMetadata());
                updateMediaDescription(mediaControllerCompat.getMetadata());
                MediaControllerCompat.setMediaController(SongPage.this, mediaControllerCompat);
                updateProgress();
                mediaControllerCompat.registerCallback(mediaControllerCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            updateMediaDescription(metadata);
            updateDuration(metadata);
        }
    }

    private void updateMediaDescription(MediaMetadataCompat metadata) {
        MediaDescriptionCompat description = metadata.getDescription();

        if (description == null) {
            return;
        }
        songName.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());
        Bitmap bitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        album_artwork.setImageBitmap(bitmap);
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        handler.postDelayed(runnable, 100);

    }

    private void stopSeekbarUpdate() {
        handler.removeCallbacks(runnable);
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        seekBar.setProgress((int) currentPosition);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);

        seekBar.setMax(duration);
        end.setText(DateUtils.formatElapsedTime(duration / 1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;

        play_pause.setImageDrawable(getDrawable(R.drawable.bigplay));

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                play_pause.setImageDrawable(getDrawable(R.drawable.bigpause));
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                stopSeekbarUpdate();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_page);

        songName = findViewById(R.id.songName);
        subtitle = findViewById(R.id.songArtist);
        seekBar = findViewById(R.id.seekbar);
        rewind = findViewById(R.id.rewind);
        forward = findViewById(R.id.forward);
        back_btn = findViewById(R.id.back_btn);
        play_pause = findViewById(R.id.play_pause);
        album_artwork = findViewById(R.id.album_artwork);

        start = findViewById(R.id.start);
        end = findViewById(R.id.end);

        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().skipToPrevious();
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().skipToNext();
            }
        });

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(SongPage.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().play();
                }
            }
        });

        initSeekBarRunnable();
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                start.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        //initSeekBarRunnable();
        token = getIntent().getParcelableExtra("token");
        mediaControllerCallback = new MediaControllerCallback();
        initBroadCasters();
    }

}
