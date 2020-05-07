package com.example.marcqtan.samplemusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */
public class MusicService extends Service {
    private SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    private List<TrackModel> tracks;
    private static final String NC_MUSIC_ID = "16069159";
    CompositeDisposable disposable;

    private MediaDescriptionCompat getMediaDescription(TrackModel track, int currentMediaIndex) {
        String album_artwork_large = null;
        if (track.artwork_url != null) {
            album_artwork_large = track.artwork_url.replace("large", "t500x500");
        }

        Bundle extras = new Bundle();
        extras.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, album_artwork_large);
        extras.putLong("currentMediaIndex", currentMediaIndex);
        extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(track.duration));
        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.genre);
        return new MediaDescriptionCompat.Builder()
                .setMediaId(track.id)
                .setTitle(track.title)
                .setSubtitle(track.genre)
                .setDescription(track.description)
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
        return null;
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
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "sample-music"));
        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();

        SCApi api = new SCApi();
        api.getService().fetchUserTracks(NC_MUSIC_ID).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<List<TrackModel>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(List<TrackModel> trackModels) {
                        TrackModel.setTrackModels(trackModels);
                        Intent i = new Intent("tracks");
                        LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(i);
                        tracks = trackModels;
                        for (TrackModel track : tracks) {
                            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse(track.stream_url + "?client_id=AIBMBzom4aIwS64tzA3uvg"));
                            concatenatingMediaSource.addMediaSource(mediaSource);
                        }
                        player.prepare(concatenatingMediaSource);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });

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
                        return tracks.get(player.getCurrentWindowIndex()).title;
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
                        return tracks.get(player.getCurrentWindowIndex()).genre;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        loadArtworkAsync(tracks.get(player.getCurrentWindowIndex()).artwork_url, callback);
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

        Intent i = new Intent("sessionToken");
        i.putExtra("Token", mediaSession.getSessionToken());
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                return MusicService.this.getMediaDescription(tracks.get(windowIndex), windowIndex);
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

                //get the position from mediaId
                if (player != null) {
                    for (int x = 0; x < tracks.size(); x++) {
                        TrackModel trackModel = tracks.get(x);
                        if (trackModel.id.equals(mediaId)) {
                            player.seekTo(x, 0);
                            player.setPlayWhenReady(true);
                            break;
                        }
                    }
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
        super.onDestroy();
    }
}
