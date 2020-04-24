package com.example.marcqtan.samplemusic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */

public class MainActivity extends AppCompatActivity {
    ListView lv;
    MediaControllerCompat mediaControllerCompat;
    MediaSessionCompat.Token token;
    Button prev, next;
    CustomAdapter adapter;
    SeekBar seekBar;
    TextView start, end, title, subtitle;
    Handler handler;
    SeekBarRunnable runnable;
    private PlaybackStateCompat mLastPlaybackState;
    ProgressBar progressBar;

    MediaControllerCallback mediaControllerCallback;

    SessionTokenBroadCastReceiver sessionReceiver;
    UIBroadCastReceiver uiReceiver;

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }

            adapter.updateButtonState(state.getState()); //update ui when clicking play/pause
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        title.setText(description.getTitle());
        subtitle.setText(description.getSubtitle());
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                progressBar.setVisibility(View.GONE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                stopSeekbarUpdate();
                progressBar.setVisibility(View.GONE);
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                progressBar.setVisibility(View.GONE);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                progressBar.setVisibility(View.VISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                progressBar.setVisibility(View.GONE);
                break;
        }
    }

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
        sessionReceiver = new SessionTokenBroadCastReceiver();
        uiReceiver = new UIBroadCastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(sessionReceiver, new IntentFilter("sessionToken"));
        LocalBroadcastManager.getInstance(this).registerReceiver(uiReceiver, new IntentFilter("broadcastIndex"));
    }
    private class SessionTokenBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if (intent.getAction().equals("sessionToken")) {
                token = intent.getParcelableExtra("Token");
                if (mediaControllerCompat == null && token != null) {
                    try {
                        mediaControllerCompat = new MediaControllerCompat(MainActivity.this, token);
                        MediaControllerCompat.setMediaController(MainActivity.this, mediaControllerCompat);
                        updatePlaybackState(mediaControllerCompat.getPlaybackState());
                        updateDuration(mediaControllerCompat.getMetadata());
                        updateMediaDescription(mediaControllerCompat.getMetadata().getDescription() );
                        mediaControllerCompat.registerCallback(mediaControllerCallback);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class UIBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("broadcastIndex")) {
                int position = intent.getIntExtra("currentPlayingIndex", -1);
                if (position >= 0) {
                    adapter.updateSelectedIndex(position); //update ui when clicking prev/next/after song end
                }
            }
        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = new Intent(this, MusicService.class);
        Util.startForegroundService(this, i);
        lv = findViewById(R.id.lv);

        ArrayList<Samples.Sample> data =
                new ArrayList<>(Arrays.asList(Samples.SAMPLES));

        adapter = new CustomAdapter(this, data);
        lv.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBar);
        seekBar = findViewById(R.id.seekbar);
        start = findViewById(R.id.start);
        end = findViewById(R.id.end);
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.album);

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
                MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToPrevious();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().skipToNext();
            }
        });

        mediaControllerCallback = new MediaControllerCallback();
        initBroadCasters();
    }


    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        handler.postDelayed(runnable, 100);

    }

    private void stopSeekbarUpdate() {
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaControllerCompat != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCompat = null;
        }

        stopSeekbarUpdate();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiReceiver);
    }

    private class CustomAdapter extends ArrayAdapter<Samples.Sample> {
        private Context ctx;
        private ArrayList<Samples.Sample> data = new ArrayList<>();
        int selectedIndex;
        int state;

        public CustomAdapter(@NonNull Context context, @NonNull ArrayList<Samples.Sample> objects) {
            super(context, 0, objects);
            this.ctx = context;
            this.data = objects;
            selectedIndex = -1;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(ctx).inflate(R.layout.song_item, parent, false);
            }

            Samples.Sample s = data.get(position);

            TextView title = view.findViewById(R.id.title);
            title.setText(s.title);


            ImageButton btn = view.findViewById(R.id.play_pause);


            btn.setImageDrawable(ctx.getDrawable(R.drawable.ic_play_arrow_black_24dp));

            if (selectedIndex == position) {
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    btn.setImageDrawable(ctx.getDrawable(R.drawable.ic_pause_black_24dp));
                } else {
                    btn.setImageDrawable(ctx.getDrawable(R.drawable.ic_play_arrow_black_24dp));
                }
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selectedIndex != position) {
                        MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(
                                Samples.SAMPLES[position].mediaId, null);
                        selectedIndex = position;
                        state = -1; //reset the state when clicking a different row
                    } else {
                        int pbState = MediaControllerCompat.getMediaController(MainActivity.this).getPlaybackState().getState();
                        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().pause();
                        } else {
                            MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().play();
                        }
                    }
                }
            });

            return view;
        }

        void updateButtonState(int state) {
            if (this.state != state && (state == PlaybackStateCompat.STATE_PLAYING
                    || state == PlaybackStateCompat.STATE_PAUSED)) {
                this.state = state;
                notifyDataSetChanged();
            }
        }

        void updateSelectedIndex(int position) {
            if(selectedIndex != position) {
                selectedIndex = position;
                notifyDataSetChanged();
            }
        }
    }
}
