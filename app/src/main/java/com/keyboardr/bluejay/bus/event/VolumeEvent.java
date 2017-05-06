package com.keyboardr.bluejay.bus.event;

import android.support.annotation.FloatRange;

/**
 * Represents a change to the player volume
 */

public class VolumeEvent {
  @FloatRange(from = 0, to = 1)
  public final float volume;

  public VolumeEvent(float volume) {
    this.volume = volume;
  }
}
