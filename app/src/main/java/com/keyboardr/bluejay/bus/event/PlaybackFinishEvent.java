package com.keyboardr.bluejay.bus.event;

/**
 * Triggered when playback is finished
 */

public class PlaybackFinishEvent {

  public final long finishTime;

  public PlaybackFinishEvent(long finishTime) {
    this.finishTime = finishTime;
  }
}
