package com.marcqtan.marcqtan.samplemusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.marcqtan.marcqtan.samplemusic.R;
import com.marcqtan.marcqtan.samplemusic.SongPage;
import com.marcqtan.marcqtan.samplemusic.fragments.MusicFragment;
import com.marcqtan.marcqtan.samplemusic.repository.TracksRepository;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */
public class MusicService extends Service {
    boolean playerPrepared = false;
    private SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    private DefaultDataSourceFactory dataSourceFactory;
    private ConcatenatingMediaSource concatenatingMediaSource;


    private List<TrackModel> tracks = new ArrayList<>();
    private static final String client_id = "AIBMBzom4aIwS64tzA3uvg";

    CompositeDisposable disposable;

    ServiceCallback serviceCallback;
    private final IBinder binder = new LocalBinder();


    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        public MusicService getService() {
            // Return this instance of MyService so clients can call public methods
            return MusicService.this;
        }
    }

    public interface ServiceCallback {
        void updateTracks(List<TrackModel> trackModels);
        void initComponents(MediaSessionCompat.Token token);
    }

    public void setServiceCallbacks(ServiceCallback serviceCallback) {
        this.serviceCallback = serviceCallback;
    }

    public void initialize() {
        serviceCallback.initComponents(mediaSession.getSessionToken());
        serviceCallback.updateTracks(tracks);
    }

    public void updateMediaSource(List<TrackModel> trackModels) {
        tracks.addAll(trackModels);
        for (TrackModel track : trackModels) {
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(track.getStream_url() + "?client_id=" + client_id));
            concatenatingMediaSource.addMediaSource(mediaSource);
        }

        if (!playerPrepared) {
            player.prepare(concatenatingMediaSource);
            playerPrepared = true;
        }
    }

    private MediaDescriptionCompat getMediaDescription(int currentMediaIndex) {
        TrackModel track = tracks.get(currentMediaIndex);

        String album_artwork_large = null;
        if (track.getArtwork_url() != null) {
            album_artwork_large = track.getArtwork_url().replace("large", "t500x500");
        }

        Bundle extras = new Bundle();
        extras.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, album_artwork_large);
        extras.putLong("currentMediaIndex", currentMediaIndex);
        extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(track.getDuration()));
        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getGenre());
        return new MediaDescriptionCompat.Builder()
                .setMediaId(track.getId())
                .setTitle(track.getTitle())
                .setSubtitle(track.getGenre())
                .setDescription(track.getDescription())
                .setExtras(extras)
                .build();
    }


    private void loadArtworkAsync(String url, PlayerNotificationManager.BitmapCallback callback) {
        Glide.with(MusicService.this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        callback.onBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = ExoPlayerFactory.newSimpleInstance(this);
        disposable = new CompositeDisposable();

        dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "sample-music"));
        concatenatingMediaSource = new ConcatenatingMediaSource();

        mediaSession = new MediaSessionCompat(this, "sample_music");
        mediaSession.setActive(true);

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                "playback_channel",
                R.string.channel_name,
                R.string.channel_description,
                1,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return tracks.get(player.getCurrentWindowIndex()).getTitle();
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(MusicService.this, SongPage.class);
                        intent.putExtra("token", mediaSession.getSessionToken());
                        return PendingIntent.getActivity(MusicService.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return tracks.get(player.getCurrentWindowIndex()).getGenre();
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        loadArtworkAsync(tracks.get(player.getCurrentWindowIndex()).getArtwork_url(), callback);
                        return null;
                    }
                }, new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        startForeground(notificationId, notification);
                    }

                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopSelf();
                    }
                });

        playerNotificationManager.setUseNavigationActionsInCompactView(true);
        playerNotificationManager.setPlayer(player);

        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());

        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                return MusicService.this.getMediaDescription(windowIndex);
            }

            @Override
            public void onSkipToPrevious(Player player, ControlDispatcher controlDispatcher) {
                super.onSkipToPrevious(player, controlDispatcher);
                player.setPlayWhenReady(true);
            }

            @Override
            public void onSkipToNext(Player player, ControlDispatcher controlDispatcher) {
                super.onSkipToNext(player, controlDispatcher);
                player.setPlayWhenReady(true);
            }
        });

        mediaSessionConnector.setPlaybackPreparer(new MediaSessionConnector.PlaybackPreparer() {
            @Override
            public long getSupportedPrepareActions() {
                return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
            }

            @Override
            public void onPrepare(boolean playWhenReady) {

            }

            @Override
            public void onPrepareFromMediaId(String mediaId, boolean playWhenReady, Bundle extras) {
                int position = 0;
                if (extras.size() > 0) {
                    position = extras.getInt("position");
                    player.seekTo(position, 0);
                    player.setPlayWhenReady(true);
                }
            }

            @Override
            public void onPrepareFromSearch(String query, boolean playWhenReady, Bundle extras) {

            }

            @Override
            public void onPrepareFromUri(Uri uri, boolean playWhenReady, Bundle extras) {

            }

            @Override
            public boolean onCommand(Player player, ControlDispatcher controlDispatcher, String command, Bundle extras, ResultReceiver cb) {
                return false;
            }
        });

        mediaSessionConnector.setPlayer(player);
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        mediaSessionConnector.setPlayer(null);
        playerNotificationManager.setPlayer(null);
        player.release();
        player = null;
        disposable.dispose();
        clear();
        super.onDestroy();
    }

    private void clear() {
        MusicFragment.switchIndex = 0;
        MusicFragment.totalVisibleItems = 0;
        TracksRepository.getInstance().resetPagenumber();
    }
}
