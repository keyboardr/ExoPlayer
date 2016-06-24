package com.keyboardr.dancedj.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;

/**
 * ViewHolder for media items
 */
public class MediaViewHolder extends RecyclerView.ViewHolder {

    public interface OnMediaItemSelectedListener {
        void onMediaItemSelected(MediaItem mediaItem);
    }

    private final TextView title;
    private final TextView artist;

    private MediaItem mediaItem;

    public MediaViewHolder(@NonNull ViewGroup parent,
                           @Nullable final OnMediaItemSelectedListener onMediaItemSelectedListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media,
                parent, false));
        title = (TextView) itemView.findViewById(R.id.media_item_title);
        artist = (TextView) itemView.findViewById(R.id.media_item_artist);

        if (onMediaItemSelectedListener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onMediaItemSelectedListener.onMediaItemSelected(mediaItem);
                }
            });
        }

    }

    public void bindMediaItem(@NonNull MediaItem mediaItem) {
        title.setText(mediaItem.title);
        artist.setText(mediaItem.artist);
        this.mediaItem = mediaItem;
    }
}
