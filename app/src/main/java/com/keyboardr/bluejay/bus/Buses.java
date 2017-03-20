package com.keyboardr.bluejay.bus;

import com.keyboardr.bluejay.bus.event.TrackIndexEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Event buses for use;
 */

public class Buses {

  // no instances
  private Buses() {
  }

  public static final EventBus MONITOR = EventBus.builder().addIndex(new Index()).build();
  public static final EventBus PLAYLIST = EventBus.builder().addIndex(new Index()).build();

  public static class PlaylistUtils {
    private PlaylistUtils(){}

    public static int getCurrentTrackIndex() {
      TrackIndexEvent event = PLAYLIST.getStickyEvent(TrackIndexEvent.class);
      return event == null ? 0 : event.newIndex;
    }
  }

}
