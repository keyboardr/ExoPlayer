package com.keyboardr.bluejay.bus.event;

import android.support.annotation.Nullable;

import com.keyboardr.bluejay.model.MediaItem;

/**
 * Represents changes to the track index
 */

public class TrackIndexEvent {

  public final int oldIndex;
  public final int newIndex;
  @Nullable
  public final MediaItem mediaItem;

  public TrackIndexEvent(int oldIndex, int newIndex, @Nullable MediaItem mediaItem) {
    this.oldIndex = oldIndex;
    this.newIndex = newIndex;
    this.mediaItem = mediaItem;
  }
}
