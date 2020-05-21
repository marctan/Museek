package com.marcqtan.marcqtan.samplemusic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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

    SongAdapter(Context ctx, Activity act, OnItemClickListener listener) {
        this.listener = listener;
        this.ctx = ctx;
        this.act = act;
        selectedIndex = -1;
    }

    void addItems(List<TrackModel> tracks) {
        this.tracks.addAll(tracks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        TrackModel track = tracks.get(position);
        TextView title = holder.title;

        holder.title.setText(track.title);
        holder.subtitle.setText(track.genre);

        ImageView btn = holder.play_pause;
        ConstraintLayout cl = holder.cl;

        btn.setImageDrawable(ctx.getDrawable(R.drawable.small_play));

        if (selectedIndex == position) {
            cl.setBackground(ctx.getDrawable(R.drawable.timeline));
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                title.setSelected(true);
                btn.setImageDrawable(ctx.getDrawable(R.drawable.small_pause));
            } else {
                title.setSelected(false);
            }
        } else {
            cl.setBackground(null);
            title.setSelected(false);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //shitty hack to fix google's bug in https://issuetracker.google.com/issues/149041228
                if(clicked == 0 && position == 0) {
                    clicked++;
                    return;
                }
                clicked = 0;
                //end of shitty hack

                if (selectedIndex != position) {
                    Bundle extras = new Bundle();
                    extras.putInt("position", position);
                    MediaControllerCompat.getMediaController(act).getTransportControls().playFromMediaId(
                            tracks.get(position).id, extras);
                    selectedIndex = position;
                    state = -1; //reset the state when clicking a different row
                } else {
                    int pbState = MediaControllerCompat.getMediaController(act).getPlaybackState().getState();
                    if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                        MediaControllerCompat.getMediaController(act).getTransportControls().pause();
                    } else {
                        MediaControllerCompat.getMediaController(act).getTransportControls().play();
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

    void updateButtonState(int state) {
        if (this.state != state && (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_PAUSED)) {
            this.state = state;
            notifyDataSetChanged();
        }
    }

    boolean updateSelectedIndex(int position) {
        if (selectedIndex != position) {
            selectedIndex = position;
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView play_pause;
        ConstraintLayout cl;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            play_pause = itemView.findViewById(R.id.play_pause);
            cl = (ConstraintLayout) itemView.findViewById(R.id.song_detail_cl);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }
    }
}
