package com.marcqtan.marcqtan.samplemusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.marcqtan.marcqtan.samplemusic.callback.MediaControllerCallback;
import com.marcqtan.marcqtan.samplemusic.databinding.SongPageBinding;
import com.marcqtan.marcqtan.samplemusic.utils.MyUtil;
import com.marcqtan.marcqtan.samplemusic.viewmodel.PlaybackViewModel;

public class SongPage extends AppCompatActivity {

    MediaControllerCallback mediaControllerCallback;
    Handler handler;
    SeekBarRunnable runnable;
    MediaSessionCompat.Token token;

    MediaControllerCompat mediaControllerCompat;

    PlaybackViewModel playbackViewModel;

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

    private void initializeCallback() {
        if (mediaControllerCompat == null && token != null) {
            try {
                mediaControllerCompat = new MediaControllerCompat(SongPage.this, token);
                playbackViewModel.updatePlaybackState(mediaControllerCompat.getPlaybackState());
                playbackViewModel.updateDuration(mediaControllerCompat.getMetadata());
                playbackViewModel.updateMediaDescription(mediaControllerCompat.getMetadata());
                MediaControllerCompat.setMediaController(SongPage.this, mediaControllerCompat);
                playbackViewModel.updateProgress();
                mediaControllerCompat.registerCallback(mediaControllerCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        handler.postDelayed(runnable, 100);

    }

    private void stopSeekbarUpdate() {
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SongPageBinding binding = SongPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mediaControllerCallback = new MediaControllerCallback(this);

        playbackViewModel = new ViewModelProvider(this).get(PlaybackViewModel.class);
        binding.songName.setSelected(true);

        playbackViewModel.getRmode().observe(this, new Observer<MyUtil.REPEAT_MODE>() {
            @Override
            public void onChanged(MyUtil.REPEAT_MODE rmode) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls()
                        .setRepeatMode(rmode.getValue());
                MyUtil.updateRepeatDrawable(rmode, binding.repeat, SongPage.this);
            }
        });

        playbackViewModel.getEnabled().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                MyUtil.updateShuffleDrawable(aBoolean, binding.shuffle, SongPage.this);
            }
        });

        playbackViewModel.getMetaData().observe(this, new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat mediaMetadataCompat) {
                binding.songName.setText(mediaMetadataCompat.getDescription().getTitle());
                binding.songArtist.setText(mediaMetadataCompat.getDescription().getSubtitle());
                String url = mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                if (url != null) {
                    Glide.with(SongPage.this).load(url).into(binding.albumArtwork);
                }
            }
        });

        playbackViewModel.getDuration().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer duration) {
                binding.seekbar.setMax(duration);
                binding.end.setText(DateUtils.formatElapsedTime(duration / 1000));
            }
        });

        playbackViewModel.getPosition().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                binding.seekbar.setProgress(aLong.intValue());
            }
        });

        playbackViewModel.getmLastPlaybackState().observe(this, new Observer<PlaybackStateCompat>() {
            @Override
            public void onChanged(PlaybackStateCompat playbackStateCompat) {
                binding.playPause.setImageDrawable(getDrawable(R.drawable.bigplay));
                if (playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    binding.playPause.setImageDrawable(getDrawable(R.drawable.bigpause));
                }
            }
        });

        playbackViewModel.getShouldRunSeekbar().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    scheduleSeekbarUpdate();
                } else {
                    stopSeekbarUpdate();
                }
            }
        });

        playbackViewModel.getShouldShowProgressbar().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });

        mediaControllerCallback.getIsSessionReady().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    MyUtil.updateRepeatDrawable(MyUtil.REPEAT_MODE.values()[mediaControllerCompat.getRepeatMode()], binding.repeat, SongPage.this);
                    MyUtil.updateShuffleDrawable(mediaControllerCompat.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL, binding.shuffle, SongPage.this);
                }
            }
        });

        mediaControllerCallback.getMetaData().observe(this, new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat metadata) {
                // null for now to avoid crash
            }
        });

        binding.repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(SongPage.this);
                playbackViewModel.repeatHandler(controllerCompat.getRepeatMode());

            }
        });

        binding.shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(SongPage.this);
                MediaControllerCompat.TransportControls controls = controllerCompat.getTransportControls();
                playbackViewModel.shuffleHandler(controls, controllerCompat);
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

        binding.rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().skipToPrevious();
            }
        });

        binding.forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().skipToNext();
            }
        });

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        binding.playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(SongPage.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    playbackViewModel.pause(MediaControllerCompat.getMediaController(SongPage.this));
                } else {
                    playbackViewModel.play(MediaControllerCompat.getMediaController(SongPage.this));
                }
            }
        });

        initSeekBarRunnable();
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
                MediaControllerCompat.getMediaController(SongPage.this).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        token = getIntent().getParcelableExtra("token");

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
