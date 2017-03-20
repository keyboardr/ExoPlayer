package com.keyboardr.bluejay.bus.event;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Posted when the Queue changes
 */

public class QueueChangeEvent {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({ChangeType.UNKNOWN, ChangeType.ADD, ChangeType.REMOVE, ChangeType.MOVE})
  public @interface ChangeType {
    int UNKNOWN = 0;
    int ADD = 1;
    int REMOVE = 2;
    int MOVE = 3;
  }

  public final List<MediaSessionCompat.QueueItem> queue;

  @ChangeType
  public final int type;

  public final int index;
  public final int oldIndex;

  public static QueueChangeEvent newInstance(@NonNull List<MediaSessionCompat.QueueItem> queue) {
    return new QueueChangeEvent(queue, ChangeType.UNKNOWN, -1, -1);
  }

  public static QueueChangeEvent add(@NonNull List<MediaSessionCompat.QueueItem> queue) {
    return new QueueChangeEvent(queue, ChangeType.ADD, queue.size() - 1, -1);
  }

  public static QueueChangeEvent remove(@NonNull List<MediaSessionCompat.QueueItem> queue,
                                        int index) {
    return new QueueChangeEvent(queue, ChangeType.REMOVE, index, -1);
  }

  public static QueueChangeEvent move(@NonNull List<MediaSessionCompat.QueueItem> queue, int
      index, int oldIndex) {
    return new QueueChangeEvent(queue, ChangeType.MOVE, index, oldIndex);
  }

  private QueueChangeEvent(List<MediaSessionCompat.QueueItem> queue,
                           @ChangeType int type, int index, int oldIndex) {
    this.queue = queue;
    this.type = type;
    this.index = index;
    this.oldIndex = oldIndex;
  }

}
