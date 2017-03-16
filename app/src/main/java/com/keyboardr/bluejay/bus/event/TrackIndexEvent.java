package com.keyboardr.bluejay.bus.event;

/**
 * Represents changes to the track index
 */

public class TrackIndexEvent {

  public final int oldIndex;
  public final int newIndex;

  public TrackIndexEvent(int oldIndex, int newIndex) {
    this.oldIndex = oldIndex;
    this.newIndex = newIndex;
  }
}
