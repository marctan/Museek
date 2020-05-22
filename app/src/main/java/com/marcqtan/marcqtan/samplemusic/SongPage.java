package com.marcqtan.marcqtan.samplemusic;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class SongPage extends AppCompatActivity {

    TextView songName, subtitle, start, end;
    ImageView rewind, forward, back_btn, play_pause, album_artwork, credits, sclogo, repeat, shuffle;
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

    private void initSeekBarRunnable() {
        handler = new Handler();
        runnable = new SeekBarRunnable();
    }

    private void initializeCallback() {
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

        @Override
        public void onSessionReady() {
            super.onSessionReady();
            MyUtil.updateRepeatDrawable(MyUtil.REPEAT_MODE.values()[MediaControllerCompat.getMediaController(SongPage.this)
                            .getRepeatMode()]
                    , repeat, SongPage.this);
            MyUtil.updateShuffleDrawable(mediaControllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL, shuffle, SongPage.this);
        }
    }

    private void updateMediaDescription(MediaMetadataCompat metadata) {
        MediaDescriptionCompat description = metadata.getDescription();

        if (description == null) {
            return;
        }
        songName.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());
        String url = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
        if (url != null) {
            Glide.with(this).load(url).into(album_artwork);
        }
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
        credits = findViewById(R.id.credits);
        sclogo = findViewById(R.id.sclogo);
        start = findViewById(R.id.start);
        end = findViewById(R.id.end);
        shuffle = findViewById(R.id.shuffle);
        repeat = findViewById(R.id.repeat);

        songName.setSelected(true);

        shuffle = findViewById(R.id.shuffle);
        repeat = findViewById(R.id.repeat);

        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(SongPage.this);
                MediaControllerCompat.TransportControls controls = controllerCompat.getTransportControls();

                MyUtil.REPEAT_MODE rmode = MyUtil.REPEAT_MODE.values()[controllerCompat.getRepeatMode()];

                if (rmode.getValue() + 1 > 2) {
                    rmode = MyUtil.REPEAT_MODE.NONE;
                } else {
                    rmode = MyUtil.REPEAT_MODE.values()[rmode.getValue() + 1];
                }

                controls.setRepeatMode(rmode.getValue());
                MyUtil.updateRepeatDrawable(rmode, repeat, SongPage.this);

            }
        });

        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = false;
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(SongPage.this);
                MediaControllerCompat.TransportControls controls = controllerCompat.getTransportControls();
                if (controllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                    controls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                } else {
                    controls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                    enabled = true;
                }

                MyUtil.updateShuffleDrawable(enabled, shuffle, SongPage.this);
            }
        });

        sclogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://soundcloud.com"));
                startActivity(intent);
            }
        });

        credits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog custoDialog = new Dialog(SongPage.this);
                custoDialog.setContentView(R.layout.credits);
                custoDialog.setCancelable(true);
                custoDialog.setCanceledOnTouchOutside(true);

                TextView tv = (TextView) custoDialog.findViewById(R.id.tv);
                String url = MediaControllerCompat.getMediaController(SongPage.this).getMetadata().getDescription().getDescription().toString();
                tv.setText(url);
                custoDialog.show();

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int displayWidth = displayMetrics.widthPixels;
                int displayHeight = displayMetrics.heightPixels;
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(custoDialog.getWindow().getAttributes());
                int dialogWindowWidth = (int) (displayWidth * 0.85f);
                int dialogWindowHeight = (int) (displayHeight * 0.75f);
                layoutParams.width = dialogWindowWidth;
                layoutParams.height = dialogWindowHeight;
                custoDialog.getWindow().setAttributes(layoutParams);

            }
        });

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
        initializeCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaControllerCompat != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCompat = null;
        }

        stopSeekbarUpdate();
    }
}
