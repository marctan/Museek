package com.example.marcqtan.samplemusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static com.example.marcqtan.samplemusic.Samples.SAMPLES;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */
public class MusicService extends Service {
    private SimpleExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    private ExoPlayerListener exoPlayerListener;

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
    public void onCreate() {
        super.onCreate();
        exoPlayerListener = new ExoPlayerListener();
        player = ExoPlayerFactory.newSimpleInstance(this);
        player.addListener(exoPlayerListener);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "sample-music"));
        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
        for (Samples.Sample sample : SAMPLES) {
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(sample.uri);
            concatenatingMediaSource.addMediaSource(mediaSource);
        }
        player.prepare(concatenatingMediaSource);

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                "playback_channel",
                R.string.channel_name,
                R.string.channel_description,
                1,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return SAMPLES[player.getCurrentWindowIndex()].title;
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(MusicService.this, MainActivity.class);
                        return PendingIntent.getActivity(MusicService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return SAMPLES[player.getCurrentWindowIndex()].description;
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return Samples.getBitmap(
                                MusicService.this, SAMPLES[player.getCurrentWindowIndex()].bitmapResource);
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

        mediaSession = new MediaSessionCompat(this, "sample_music");
        mediaSession.setActive(true);

        playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());

        Intent i = new Intent("sessionToken");
        i.putExtra("Token", mediaSession.getSessionToken());
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setQueueNavigator(new TimelineQueueNavigator(mediaSession) {
            @Override
            public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
                return Samples.getMediaDescription(MusicService.this, SAMPLES[windowIndex]);
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
                if(player != null) {
                    for (int x = 0; x < SAMPLES.length; x++) {
                        Samples.Sample s = SAMPLES[x];
                        if (s.mediaId.equals(mediaId)) {
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
        player.removeListener(exoPlayerListener);
        player.release();
        player = null;

        super.onDestroy();
    }

    private class ExoPlayerListener implements Player.EventListener {
        @Override
        public void onPositionDiscontinuity(int reason) {
            Intent i = new Intent("broadcastIndex");
            i.putExtra("currentPlayingIndex", player.getCurrentWindowIndex());
            LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(i);
        }
    }
}
