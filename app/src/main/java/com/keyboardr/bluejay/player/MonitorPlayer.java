package com.keyboardr.bluejay.player;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.model.MediaItem;

import org.greenrobot.eventbus.EventBus;

/**
 * A wrapper around the media player for Monitor playback.
 */

public class MonitorPlayer extends AbsPlayer {

  public static final String ACTION_MONITOR_TRACK_CHANGED = "com.keyboardr.bluejay.player"
      + ".MonitorPlayer.ACTION_MONITOR_TRACK_CHANGED";

  private static final String TAG = "MonitorPlayer";

  private MediaItem currentItem;

  public MonitorPlayer(@NonNull Context context) {
    super(context, C.STREAM_TYPE_MUSIC);
  }


  public void play(MediaItem mediaItem, boolean playWhenReady) {
    SimpleExoPlayer player = ensurePlayer();
    player.prepare(getMediaSource(mediaItem));
    currentItem = mediaItem;
    getBus().postSticky(new TrackIndexEvent(-1, -1, currentItem));
    player.setPlayWhenReady(playWhenReady);
    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent
        (ACTION_MONITOR_TRACK_CHANGED));
  }

  @SuppressWarnings("WeakerAccess")
  public void resume() {
    SimpleExoPlayer player = ensurePlayer();
    if (player.getPlaybackState() == ExoPlayer.STATE_ENDED) {
      player.seekTo(0);
    }
    player.setPlayWhenReady(true);
  }

  @SuppressWarnings("WeakerAccess")
  public void pause() {
    ensurePlayer().setPlayWhenReady(false);
  }

  @Override
  public void togglePlayPause() {
    if (isPaused() || isStopped()) {
      resume();
    } else {
      pause();
    }
  }

  @Override
  public MediaItem getCurrentMediaItem() {
    return currentItem;
  }

  @SuppressWarnings("unused")
  public void stop() {
    ensurePlayer().stop();
  }

  public void seekTo(long positionMs) {
    ensurePlayer().seekTo(positionMs);
  }

  @Override
  protected EventBus getBus() {
    return Buses.MONITOR;
  }
}
