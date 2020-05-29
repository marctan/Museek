package com.marcqtan.marcqtan.samplemusic.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.marcqtan.marcqtan.samplemusic.R;
import com.marcqtan.marcqtan.samplemusic.tracksmodel.TrackModel;
import com.marcqtan.marcqtan.samplemusic.databinding.SongListItemBinding;
import com.marcqtan.marcqtan.samplemusic.viewmodel.PlaybackViewModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Marc Q. Tan on 07/05/2020.
 */

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.MyViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;
    private Context ctx;
    private Activity act;
    private List<TrackModel> tracks = new ArrayList<>();
    private int selectedIndex;
    private int state;
    int clicked = 0;

    private PlaybackViewModel playbackViewModel;

    public SongAdapter(Context ctx, Activity act, OnItemClickListener listener, ViewModelStoreOwner owner) {
        this.listener = listener;
        this.ctx = ctx;
        this.act = act;
        selectedIndex = -1;
        playbackViewModel = new ViewModelProvider(owner).get(PlaybackViewModel.class);
    }

    public void addItems(List<TrackModel> tracks) {
        this.tracks.addAll(tracks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SongListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.song_list_item, parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        TrackModel track = tracks.get(position);
        holder.bind(track);

        ImageView btn = holder.binding.playPause;
        ConstraintLayout cl = holder.binding.songDetailCl;

        btn.setImageDrawable(ctx.getDrawable(R.drawable.small_play));

        if (selectedIndex == position) {
            cl.setBackground(ctx.getDrawable(R.drawable.timeline));
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                holder.binding.title.setSelected(true);
                btn.setImageDrawable(ctx.getDrawable(R.drawable.small_pause));
            } else {
                holder.binding.title.setSelected(false);
            }
        } else {
            cl.setBackground(null);
            holder.binding.title.setSelected(false);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //shitty hack to fix google's bug in https://issuetracker.google.com/issues/149041228
                if (clicked == 0 && position == 0) {
                    clicked++;
                    return;
                }
                clicked = 0;
                //end of shitty hack

                if (selectedIndex != position) {
                    Bundle extras = new Bundle();
                    extras.putInt("position", position);
                    playbackViewModel.playFromMedia(MediaControllerCompat.getMediaController(act), tracks.get(position).getId(), extras);
                    selectedIndex = position;
                    state = -1; //reset the state when clicking a different row
                } else {
                    int pbState = MediaControllerCompat.getMediaController(act).getPlaybackState().getState();
                    if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                        playbackViewModel.pause(MediaControllerCompat.getMediaController(act));
                    } else {
                        playbackViewModel.play(MediaControllerCompat.getMediaController(act));
                    }
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        if (tracks == null) {
            return 0;
        }
        return tracks.size();
    }

    public void updateButtonState(int state) {
        if (this.state != state && (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_PAUSED)) {
            this.state = state;
            notifyDataSetChanged();
        }
    }

    public void updateSelectedIndex(int position) {
        if (selectedIndex != position) {
            selectedIndex = position;
            notifyDataSetChanged();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        SongListItemBinding binding;

        MyViewHolder(@NonNull SongListItemBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }

        public void bind(TrackModel trackModel) {
            binding.setTrack(trackModel);
            binding.executePendingBindings();
        }
    }
}
