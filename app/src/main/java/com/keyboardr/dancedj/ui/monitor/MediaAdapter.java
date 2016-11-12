package com.keyboardr.dancedj.ui.monitor;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.ui.recycler.MediaViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Keyboardr on 11/11/2016.
 */

public class MediaAdapter extends RecyclerView.Adapter<MediaViewHolder> {

    private final MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener;
    private final MediaViewHolder.MediaViewDecorator mediaViewDecorator;
    private ArrayList<MediaItem> mediaItems;

    public MediaAdapter(@Nullable MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener,
                        @Nullable MediaViewHolder.MediaViewDecorator mediaViewDecorator) {
        this.mediaItemSelectedListener = mediaItemSelectedListener;
        this.mediaViewDecorator = mediaViewDecorator;
        setHasStableIds(true);
    }

    public void setMediaItems(List<MediaItem> mediaItems) {
        this.mediaItems = new ArrayList<>(mediaItems);
        notifyDataSetChanged();
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaViewHolder(parent, mediaItemSelectedListener, mediaViewDecorator);
    }

    @Override
    public void onBindViewHolder(MediaViewHolder holder, int position) {
        holder.bindMediaItem(mediaItems.get(position), false, true);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mediaItems.get(position).getTransientId();
    }
}
