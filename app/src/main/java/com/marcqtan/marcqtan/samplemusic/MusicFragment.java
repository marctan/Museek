package com.marcqtan.marcqtan.samplemusic;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Marc Q. Tan on 08/05/2020.
 */
public class MusicFragment extends Fragment implements SongAdapter.OnItemClickListener, MainActivity.OnFragmentReselected, MusicService.ServiceCallback {


    private RecyclerView rv;
    private MediaControllerCompat mediaControllerCompat;
    private MediaSessionCompat.Token token;
    private ImageView prev, next, album_artwork, credits, sclogo;
    private SongAdapter adapter;
    private SeekBar seekBar;
    private TextView start, end, title;
    private Handler handler;
    private SeekBarRunnable runnable;
    private PlaybackStateCompat mLastPlaybackState;
    private ProgressBar progressBar, startUpProgressBar;

    private MediaControllerCallback mediaControllerCallback;

    private static int totalVisibleItems = 0;
    private int currentIndex = 0;
    private static int switchIndex = 0;

    private boolean loading = false;
    private final int VISIBLE_THRESHOLD = 1;
    private int lastVisibleItem, totalItemCount;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private LinearLayoutManager linearLayoutManager;
    private MusicService musicService;
    private boolean mShouldUnbind;
    private List<TrackModel> trackModels = new ArrayList<>();

    private RVScrollListener rvScrollListener;

    private class RVScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView,
                               int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            totalItemCount = linearLayoutManager.getItemCount();
            lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
            if (!loading && totalItemCount <= (lastVisibleItem + VISIBLE_THRESHOLD)) {
                musicService.getPaginator().onNext(musicService.getPageNumber());
                loading = true;
            }
        }

        ;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setOnFragmentReselectedListener(this);
    }

    @Override
    public void onItemClick(int position) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(requireActivity());
        MediaDescriptionCompat description = controller.getMetadata().getDescription();
        String mediaId = trackModels.get(position).id;

        Bundle extras = new Bundle();
        extras.putInt("position", position);
        if (!mediaId.equals(description.getMediaId())) {
            controller.getTransportControls().playFromMediaId(mediaId, extras);
            return;
        }

        Intent intent = new Intent(requireActivity(), SongPage.class);
        intent.putExtra("media_description", description);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    @Override
    public void onReselect() {
        rv.scrollToPosition(currentIndex < totalVisibleItems ? 0 : currentIndex);
        View v = getView();
        if (v != null) {
            MotionLayout ml = ((MotionLayout) v.findViewById(R.id.main_layout));
            if (currentIndex >= totalVisibleItems - 1) {
                ml.transitionToEnd();
            } else {
                ml.transitionToStart();
            }
        }
    }

    @Override
    public void updateTracks(List<TrackModel> trackModels) {

        if (trackModels == null || trackModels.size() == 0)
            return;

        this.trackModels.addAll(trackModels);
        adapter.addItems(trackModels);

        currentIndex = (int) mediaControllerCompat.getMetadata().getLong("currentMediaIndex");
        adapter.updateSelectedIndex(currentIndex);

        if (currentIndex >= totalVisibleItems - 1) { //when switching tab, scroll to playing item
            View v = getView();
            if (v != null) {
                rv.scrollToPosition(currentIndex);
            }
        }

        startUpProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void initComponents(MediaSessionCompat.Token token) {
        this.token = token;
        if (mediaControllerCompat == null && token != null) {
            try {
                mediaControllerCompat = new MediaControllerCompat(requireActivity(), token);
                MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat);
                updatePlaybackState(mediaControllerCompat.getPlaybackState());
                updateDuration(mediaControllerCompat.getMetadata());
                updateProgress();
                updateMediaDescription(mediaControllerCompat.getMetadata());
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
            boolean updated = adapter.updateSelectedIndex((int) metadata.getLong("currentMediaIndex"));
            currentIndex = (int) mediaControllerCompat.getMetadata().getLong("currentMediaIndex");
            switchIndex = currentIndex;
            if (updated) { //scroll to the currently playing song.
                onReselect();
            }
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


    private void setUpLoadMoreListener() {
        rvScrollListener = new RVScrollListener();
        rv.addOnScrollListener(rvScrollListener);
    }

    @Override
    public void loadItem() {
        loading = true;
        startUpProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void loadAfter(List<TrackModel> trackModels) {
        startUpProgressBar.setVisibility(View.GONE);
        this.trackModels.addAll(trackModels);
        if (trackModels.size() == 0) { //all songs have been fetched
            rv.removeOnScrollListener(rvScrollListener);
            return;
        }
        adapter.addItems(trackModels);

        if (totalVisibleItems == 0) { //do only once
            rv.post(new Runnable() {
                @Override
                public void run() {
                    totalVisibleItems = rv.getChildCount();
                }
            });
        }

        loading = false;
    }

    @Override
    public void showDialog(PublishProcessor<Integer> paginator, int pageNumber) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Error!");
        builder.setMessage("Error encountered while fetching tracks. Retry?");
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requireActivity().finishAndRemoveTask();
            }
        });
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                musicService.subscribeForData();
            }
        });
        builder.show();
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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_music_list, container, false);

        mediaControllerCallback = new MediaControllerCallback();
        linearLayoutManager = new LinearLayoutManager(requireContext());
        rv = v.findViewById(R.id.rv);


        adapter = new SongAdapter(getContext(), requireActivity(), this);
        rv.setAdapter(adapter);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(linearLayoutManager);

        progressBar = v.findViewById(R.id.progressBar);
        startUpProgressBar = v.findViewById(R.id.startUpProgressBar);

        seekBar = v.findViewById(R.id.seekbar);
        start = v.findViewById(R.id.start);
        end = v.findViewById(R.id.end);
        title = v.findViewById(R.id.songName);
        seekBar.setEnabled(false); //set to true to show
        title.setSelected(true);

        setUpLoadMoreListener();

        if (totalVisibleItems > 0) { //we need this to avoid flicker of transition
            if (switchIndex > totalVisibleItems - 1) {
                ((MotionLayout) v.findViewById(R.id.main_layout)).setProgress(1);
            }
        }

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
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        prev = v.findViewById(R.id.prev);
        next = v.findViewById(R.id.next);
        album_artwork = v.findViewById(R.id.album_artwork);
        credits = v.findViewById(R.id.credits);
        sclogo = v.findViewById(R.id.sclogo);

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
                Dialog custoDialog = new Dialog(requireActivity());
                custoDialog.setContentView(R.layout.credits);
                custoDialog.setCancelable(true);
                custoDialog.setCanceledOnTouchOutside(true);

                TextView tv = (TextView) custoDialog.findViewById(R.id.tv);
                String url = MediaControllerCompat.getMediaController(requireActivity()).getMetadata().getDescription().getDescription().toString();
                tv.setText(url);
                custoDialog.show();

                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
                Intent intent = new Intent(requireActivity(), SongPage.class);
                intent.putExtra("media_description",
                        MediaControllerCompat.getMediaController(requireActivity()).getMetadata().getDescription());
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().skipToPrevious();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().skipToNext();
            }
        });
        initSeekBarRunnable();

        Intent i = new Intent(requireActivity(), MusicService.class);
        Util.startForegroundService(requireActivity(), i);
        requireActivity().bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);

        return v;
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        handler.postDelayed(runnable, 100);

    }

    private void stopSeekbarUpdate() {
        handler.removeCallbacks(runnable);
    }


    /**
     * Callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            musicService.setServiceCallbacks(MusicFragment.this);
            musicService.initialize();
            musicService.subscribeForData();
            mShouldUnbind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
            mShouldUnbind = false;
        }

    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaControllerCompat != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCompat = null;
        }

        stopSeekbarUpdate();
        compositeDisposable.dispose();
        if (mShouldUnbind) {
            musicService.setServiceCallbacks(null);
            requireActivity().unbindService(serviceConnection);
        }
    }

    public static MusicFragment newInstance() {
        MusicFragment fragment = new MusicFragment();
        return fragment;
    }
}
