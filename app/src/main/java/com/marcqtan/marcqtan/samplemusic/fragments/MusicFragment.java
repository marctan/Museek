package com.marcqtan.marcqtan.samplemusic.fragments;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.marcqtan.marcqtan.samplemusic.MainActivity;
import com.marcqtan.marcqtan.samplemusic.callback.MediaControllerCallback;
import com.marcqtan.marcqtan.samplemusic.service.MusicService;
import com.marcqtan.marcqtan.samplemusic.utils.MyUtil;
import com.marcqtan.marcqtan.samplemusic.R;
import com.marcqtan.marcqtan.samplemusic.adapter.SongAdapter;
import com.marcqtan.marcqtan.samplemusic.SongPage;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackCollection;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackModel;
import com.marcqtan.marcqtan.samplemusic.databinding.MainMusicListBinding;
import com.marcqtan.marcqtan.samplemusic.viewmodel.PlaybackViewModel;
import com.marcqtan.marcqtan.samplemusic.viewmodel.TracksViewModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Marc Q. Tan on 08/05/2020.
 */
public class MusicFragment extends Fragment implements SongAdapter.OnItemClickListener, MainActivity.OnFragmentReselected, MusicService.ServiceCallback {

    private MainMusicListBinding binding;

    private MediaControllerCompat mediaControllerCompat;
    private MediaSessionCompat.Token token;
    private SongAdapter adapter;
    private Handler handler;
    private SeekBarRunnable runnable;

    private MediaControllerCallback mediaControllerCallback;

    public static int totalVisibleItems = 0;
    private int currentIndex = 0;
    public static int switchIndex = 0;

    private boolean loading = false;
    private final int VISIBLE_THRESHOLD = 1;
    private int lastVisibleItem, totalItemCount;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private LinearLayoutManager linearLayoutManager;
    private MusicService musicService;
    private boolean mShouldUnbind;
    private List<TrackModel> trackModels = new ArrayList<>();

    private TracksViewModel tracksViewModel;
    private PlaybackViewModel playbackViewModel;

    private RVScrollListener rvScrollListener;

    private class RVScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView,
                               int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            totalItemCount = linearLayoutManager.getItemCount();
            lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
            if (!loading && totalItemCount <= (lastVisibleItem + VISIBLE_THRESHOLD)) {
                loadItem();
                tracksViewModel.queryTracks();
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
        String mediaId = trackModels.get(position).getId();

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
        binding.rv.scrollToPosition(currentIndex < totalVisibleItems ? 0 : currentIndex);
        View v = getView();
        if (v != null) {
            MotionLayout ml = ((MotionLayout) binding.mainLayout);
            if (currentIndex >= totalVisibleItems - 1) {
                ml.transitionToEnd();
            } else {
                ml.transitionToStart();
            }
        }
    }

    @Override
    public void updateTracks(List<TrackModel> trackModels) {
        tracksViewModel.updateTracks(trackModels);
    }

    @Override
    public void initComponents(MediaSessionCompat.Token token) {
        this.token = token;
        if (mediaControllerCompat == null && token != null) {
            try {
                mediaControllerCompat = new MediaControllerCompat(requireActivity(), token);
                MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat);
                playbackViewModel.updatePlaybackState(mediaControllerCompat.getPlaybackState());
                playbackViewModel.updateDuration(mediaControllerCompat.getMetadata());
                playbackViewModel.updateProgress();
                playbackViewModel.updateMediaDescription(mediaControllerCompat.getMetadata());
                mediaControllerCompat.registerCallback(mediaControllerCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaControllerCompat != null) {
            MyUtil.updateRepeatDrawable(MyUtil.REPEAT_MODE.values()[mediaControllerCompat.getRepeatMode()], binding.repeat, requireContext());
            MyUtil.updateShuffleDrawable(mediaControllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL, binding.shuffle, requireContext());
        }
    }

    private class SeekBarRunnable implements Runnable {
        @Override
        public void run() {
            playbackViewModel.updateProgress();
            handler.postDelayed(this, 1000);
        }
    }

    private void initSeekBarRunnable() {
        handler = new Handler();
        runnable = new SeekBarRunnable();
    }


    private void setUpLoadMoreListener() {
        rvScrollListener = new RVScrollListener();
        binding.rv.addOnScrollListener(rvScrollListener);
    }

    private void loadItem() {
        loading = true;
        binding.startUpProgressBar.setVisibility(View.VISIBLE);
    }

    private void loadAfter(List<TrackModel> trackModels) {
        binding.startUpProgressBar.setVisibility(View.GONE);

        if(trackModels == null) {
            loading = false;
            return;
        }

        this.trackModels.addAll(trackModels);
        if (trackModels.size() == 0) { //all songs have been fetched
            binding.rv.removeOnScrollListener(rvScrollListener);
            return;
        }
        adapter.addItems(trackModels);

        if (totalVisibleItems == 0) { //do only once
            binding.rv.post(new Runnable() {
                @Override
                public void run() {
                    totalVisibleItems = binding.rv.getChildCount();
                }
            });
        }

        loading = false;
    }

    private void showDialog() {
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
                loadItem();
                tracksViewModel.queryTracks();
            }
        });
        builder.show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        playbackViewModel = new ViewModelProvider(this).get(PlaybackViewModel.class);
        tracksViewModel = new ViewModelProvider(this).get(TracksViewModel.class);

        tracksViewModel.getCollectionTracks().observe(getViewLifecycleOwner(), new Observer<TrackCollection>() {
            @Override
            public void onChanged(TrackCollection trackCollection) {
                if ("error".equals(trackCollection.getNext_href())) {
                    Log.d("Error", "Error fetching tracks!");
                    showDialog();
                } else {
                    musicService.updateMediaSource(trackCollection.getTracks());
                }
                loadAfter(trackCollection.getTracks());
            }
        });

        tracksViewModel.getLoadedTracks().observe(getViewLifecycleOwner(), new Observer<List<TrackModel>>() {
            @Override
            public void onChanged(List<TrackModel> trackModels) {
                if (trackModels == null || trackModels.size() == 0)
                    return;

                MusicFragment.this.trackModels.addAll(trackModels);
                adapter.addItems(trackModels);

                currentIndex = (int) mediaControllerCompat.getMetadata().getLong("currentMediaIndex");
                adapter.updateSelectedIndex(currentIndex);

                if (currentIndex >= totalVisibleItems - 1) { //when switching tab, scroll to playing item
                    View v = getView();
                    if (v != null) {
                        binding.rv.scrollToPosition(currentIndex);
                    }
                }
                binding.startUpProgressBar.setVisibility(View.GONE);
            }
        });

        playbackViewModel.getmLastPlaybackState().observe(getViewLifecycleOwner(), new Observer<PlaybackStateCompat>() {
            @Override
            public void onChanged(PlaybackStateCompat playbackStateCompat) {
                adapter.updateButtonState(playbackStateCompat.getState());
            }
        });

        playbackViewModel.getShouldRunSeekbar().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    scheduleSeekbarUpdate();
                } else {
                    stopSeekbarUpdate();
                }
            }
        });

        playbackViewModel.getShouldShowProgressbar().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });

        playbackViewModel.getPosition().observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                binding.seekbar.setProgress(aLong.intValue());
            }
        });

        playbackViewModel.getDuration().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer duration) {
                binding.seekbar.setMax(duration);
                binding.end.setText(DateUtils.formatElapsedTime(duration / 1000));
            }
        });

        playbackViewModel.getMetaData().observe(getViewLifecycleOwner(), new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat mediaMetadataCompat) {
                binding.songName.setText(mediaMetadataCompat.getDescription().getTitle());
                String url = mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                if (url != null) {
                    Glide.with(MusicFragment.this).load(url).into(binding.albumArtwork);
                }
            }
        });

        playbackViewModel.getRmode().observe(getViewLifecycleOwner(), new Observer<MyUtil.REPEAT_MODE>() {
            @Override
            public void onChanged(MyUtil.REPEAT_MODE rmode) {
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls()
                        .setRepeatMode(rmode.getValue());
                MyUtil.updateRepeatDrawable(rmode, binding.repeat, requireContext());
            }
        });

        playbackViewModel.getEnabled().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                MyUtil.updateShuffleDrawable(aBoolean, binding.shuffle, requireContext());
            }
        });

        mediaControllerCallback.getMetaData().observe(getViewLifecycleOwner(), new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat metadata) {
                adapter.updateSelectedIndex((int) metadata.getLong("currentMediaIndex"));
                currentIndex = (int) mediaControllerCompat.getMetadata().getLong("currentMediaIndex");
                if (switchIndex != currentIndex) { //scroll to the currently playing song.
                    onReselect();
                }
                switchIndex = currentIndex;
            }
        });

        mediaControllerCallback.getIsSessionReady().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    MyUtil.updateRepeatDrawable(MyUtil.REPEAT_MODE.values()[mediaControllerCompat.getRepeatMode()], binding.repeat, requireContext());
                    MyUtil.updateShuffleDrawable(mediaControllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL, binding.shuffle, requireContext());
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MainMusicListBinding.inflate(getLayoutInflater());
        View v = binding.getRoot();

        mediaControllerCallback = new MediaControllerCallback(this);
        linearLayoutManager = new LinearLayoutManager(requireContext());

        binding.repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(requireActivity());
                playbackViewModel.repeatHandler(controllerCompat.getRepeatMode());
            }
        });

        binding.shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(requireActivity());
                MediaControllerCompat.TransportControls controls = controllerCompat.getTransportControls();
                playbackViewModel.shuffleHandler(controls, controllerCompat);
            }
        });


        adapter = new SongAdapter(getContext(), requireActivity(), this, this);
        binding.rv.setAdapter(adapter);
        binding.rv.setHasFixedSize(true);
        binding.rv.setLayoutManager(linearLayoutManager);

        binding.seekbar.setEnabled(false); //set to true to show
        binding.songName.setSelected(true);

        setUpLoadMoreListener();

        if (totalVisibleItems > 0) { //we need this to avoid flicker of transition
            if (switchIndex >= totalVisibleItems - 1) {
                ((MotionLayout) binding.mainLayout).setProgress(1);
            }
        }

        binding.seekbar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                binding.start.setText(DateUtils.formatElapsedTime(progress / 1000));
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

        binding.sclogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://soundcloud.com"));
                startActivity(intent);
            }
        });

        binding.credits.setOnClickListener(new View.OnClickListener() {
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

        binding.albumArtwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireActivity(), SongPage.class);
                intent.putExtra("media_description",
                        MediaControllerCompat.getMediaController(requireActivity()).getMetadata().getDescription());
                intent.putExtra("token", token);
                startActivity(intent);
            }
        });

        binding.prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().skipToPrevious();
            }
        });

        binding.next.setOnClickListener(new View.OnClickListener() {
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

            if (totalVisibleItems == 0) {//fetch track once service is connected
                loadItem();
                tracksViewModel.queryTracks();
            }

            musicService = binder.getService();
            musicService.setServiceCallbacks(MusicFragment.this);
            musicService.initialize();

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
