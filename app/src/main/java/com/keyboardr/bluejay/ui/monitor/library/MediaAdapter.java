package com.keyboardr.bluejay.ui.monitor.library;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaViewHolder> {

  private final MediaViewHolder.MediaViewDecorator mediaViewDecorator;
  private ArrayList<MediaItem> mediaItems;

  public MediaAdapter(@Nullable MediaViewHolder.MediaViewDecorator mediaViewDecorator) {
    this.mediaViewDecorator = mediaViewDecorator;
    setHasStableIds(true);
  }

  public void setMediaItems(List<MediaItem> mediaItems) {
    this.mediaItems = new ArrayList<>(mediaItems);
    notifyDataSetChanged();
  }

  @Override
  public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new MediaViewHolder(parent, mediaViewDecorator);
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
