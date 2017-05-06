package com.keyboardr.bluejay.bus.event;

import com.keyboardr.bluejay.bus.Buses;

public class MediaConnectedEvent {
  private final boolean connected;

  public MediaConnectedEvent(boolean connected) {
    this.connected = connected;
  }

  public static boolean isConnected() {
    MediaConnectedEvent event = Buses.PLAYLIST.getStickyEvent(MediaConnectedEvent.class);
    return event != null && event.connected;
  }
}
