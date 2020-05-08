package com.example.marcqtan.samplemusic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */

public class MainActivity extends AppCompatActivity implements SongAdapter.OnItemClickListener {
    RecyclerView rv;
    MediaControllerCompat mediaControllerCompat;
    MediaSessionCompat.Token token;
    ImageView prev, next, album_artwork, credits;
    SongAdapter adapter;
    SeekBar seekBar;
    TextView start, end, title;
    Handler handler;
    SeekBarRunnable runnable;
    private PlaybackStateCompat mLastPlaybackState;
    ProgressBar progressBar;

    MediaControllerCallback mediaControllerCallback;

    SessionTokenBroadCastReceiver sessionReceiver;
    TrackBroadCastReceiver trackReceiver;

    @Override
    public void onItemClick(int position) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(MainActivity.this);
        MediaDescriptionCompat description = controller.getMetadata().getDescription();
        String mediaId = TrackModel.getTrackModels().get(position).id;

        if (!mediaId.equals(description.getMediaId())) {
            controller.getTransportControls().playFromMediaId(mediaId, null);
            return;
        }

        Intent intent = new Intent(MainActivity.this, SongPage.class);
        intent.putExtra("media_description", description);
        intent.putExtra("token", token);
        startActivity(intent);
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
            adapter.updateSelectedIndex((int) metadata.getLong("currentMediaIndex"));
        }
    }

    private void updateMediaDescription(MediaMetadataCompat metadata) {
        MediaDescriptionCompat description = metadata.getDescription();
        if (description == null) {
            return;
        }
        title.setText(description.getTitle());
        String url = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
        if (url != null) {
            Glide.with(this).load(url).into(album_artwork);
        }
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }

        adapter.updateButtonState(state.getState());
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

    private void initSeekBarRunnable() {
        handler = new Handler();
        runnable = new SeekBarRunnable();
    }

    private void initBroadCasters() {
        sessionReceiver = new SessionTokenBroadCastReceiver();
        trackReceiver = new TrackBroadCastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(sessionReceiver, new IntentFilter("sessionToken"));
        LocalBroadcastManager.getInstance(this).registerReceiver(trackReceiver, new IntentFilter("tracks"));
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
                        updateMediaDescription(mediaControllerCompat.getMetadata());
                        mediaControllerCompat.registerCallback(mediaControllerCallback);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class TrackBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("tracks".equals(intent.getAction())) {
                adapter.setItem(TrackModel.getTrackModels());
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
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music_list);

        mediaControllerCallback = new MediaControllerCallback();
        initBroadCasters();
        initSeekBarRunnable();

        Intent i = new Intent(MainActivity.this, MusicService.class);
        Util.startForegroundService(MainActivity.this, i);
        rv = findViewById(R.id.rv);

        adapter = new SongAdapter(this, this, this);
        rv.setAdapter(adapter);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));

        progressBar = findViewById(R.id.progressBar);

        seekBar = findViewById(R.id.seekbar);
        start = findViewById(R.id.start);
        end = findViewById(R.id.end);
        title = findViewById(R.id.songName);
        seekBar.setEnabled(false); //set to true to show

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
        album_artwork = findViewById(R.id.album_artwork);
        credits = findViewById(R.id.credits);

        credits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog custoDialog = new Dialog(MainActivity.this);
                custoDialog.setContentView(R.layout.credits);
                custoDialog.setCancelable(true);
                custoDialog.setCanceledOnTouchOutside(true);

                TextView tv = (TextView) custoDialog.findViewById(R.id.tv);
                String url = MediaControllerCompat.getMediaController(MainActivity.this).getMetadata().getDescription().getDescription().toString();
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

        album_artwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SongPage.class);
                intent.putExtra("media_description",
                        MediaControllerCompat.getMediaController(MainActivity.this).getMetadata().getDescription());
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackReceiver);
    }
}
