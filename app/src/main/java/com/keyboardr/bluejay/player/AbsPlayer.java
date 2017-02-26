package com.keyboardr.bluejay.player;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.keyboardr.bluejay.model.MediaItem;

import java.io.IOException;

/**
 * A wrapper around the media player for Monitor playback.
 */

public abstract class AbsPlayer implements Player {

  private static final String TAG = "Player";
  @NonNull
  protected final Context context;
  private final Handler mainHandler;
  @C.StreamType
  private final int audioStreamType;

  @Nullable
  protected PlaybackListener playbackListener;

  private final DefaultDataSourceFactory defaultDataSourceFactory;
  private final DefaultExtractorsFactory defaultExtractorsFactory;
  private final ExtractorMediaSource.EventListener extractorListener = new ExtractorMediaSource
      .EventListener() {
    @Override
    public void onLoadError(IOException error) {
      Log.e(TAG, "onLoadError: ", error);
    }
  };
  private final ExoPlayer.EventListener playerListener = new ExoPlayer.EventListener() {

    @Override
    public void onLoadingChanged(boolean isLoading) {
      Log.d(TAG, "onLoadingChanged() called with: isLoading = [" + isLoading + "]");
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(AbsPlayer.this);
      }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
      Log.d(TAG, "onPlayerStateChanged() called with: playWhenReady = [" + playWhenReady + "], "
          + "playbackState = [" + playbackState + "]");
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(AbsPlayer.this);
      }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
      Log.d(TAG, "onTimelineChanged() called with: timeline = [" + timeline + "], manifest = [" +
          manifest + "]");
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(AbsPlayer.this);
      }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
      Log.d(TAG, "onTracksChanged() called with: trackGroups = [" + trackGroups + "], "
          + "trackSelections = [" + trackSelections + "]");
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(AbsPlayer.this);
      }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
      Log.e(TAG, "onPlayerError: ", error);
    }

    @Override
    public void onPositionDiscontinuity() {
      Log.d(TAG, "onPositionDiscontinuity() called");
      if (playbackListener != null) {
        playbackListener.onSeekComplete(AbsPlayer.this);
      }
    }
  };
  @Nullable
  private SimpleExoPlayer player;

  public AbsPlayer(@NonNull Context context, @C.StreamType int audioStreamType) {
    this.context = context;
    this.audioStreamType = audioStreamType;
    mainHandler = new Handler();
    defaultDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context,
        "DanceDJ"));
    defaultExtractorsFactory = new DefaultExtractorsFactory();
    ensurePlayer();
  }

  @NonNull
  protected SimpleExoPlayer ensurePlayer() {
    if (player != null) {
      return player;
    }
    player = ExoPlayerFactory.newSimpleInstance(context,
        new DefaultTrackSelector(),
        new DefaultLoadControl());
    player.setAudioStreamType(audioStreamType);
    player.addListener(playerListener);
    return player;
  }

  @Override
  public void setPlaybackListener(@Nullable PlaybackListener playbackListener) {
    this.playbackListener = playbackListener;
  }

  @Override
  public void setAudioOutput(@Nullable AudioDeviceInfo audioDeviceInfo) {
    ensurePlayer().setAudioOutput(audioDeviceInfo);
  }

  @Override
  public int getAudioOutputType() {
    if (player == null) {
      return -1;
    }
    AudioDeviceInfo audioOutput = player.getAudioOutput();
    return audioOutput != null ? audioOutput.getType() : AudioDeviceInfo.TYPE_UNKNOWN;
  }

  @NonNull
  protected MediaSource getMediaSource(MediaItem mediaItem) {
    return new ExtractorMediaSource(mediaItem.toUri(), defaultDataSourceFactory,
        defaultExtractorsFactory, mainHandler, extractorListener);
  }

  @Override
  public void release() {
    if (player != null) {
      player.removeListener(playerListener);
      player.release();
      player = null;
    }
  }

  @Override
  public int getPlayState() {
    if (isPlaying()) {
      return PlayState.PLAYING;
    }
    if (isPaused()) {
      return PlayState.PAUSED;
    }
    if (isLoading()) {
      return PlayState.LOADING;
    }
    if (isStopped()) {
      return PlayState.STOPPED;
    }
    if (player != null) {
      Log.e(TAG, "Unknown play state. Playback state: " + player.getPlaybackState());
    } else {
      Log.e(TAG, "Unknown play state. Player null");
    }
    return PlayState.STOPPED;
  }

  @Override
  public boolean isPlaying() {
    return player != null && player.getPlaybackState() == ExoPlayer.STATE_READY && player
        .getPlayWhenReady();
  }

  @Override
  public boolean isPaused() {
    return player != null && player.getPlaybackState() == ExoPlayer.STATE_READY && !player
        .getPlayWhenReady();
  }

  @Override
  public boolean isLoading() {
    return player != null && (player.isLoading() || player.getPlaybackState() == ExoPlayer
        .STATE_BUFFERING);
  }

  @Override
  public boolean isStopped() {
    return player == null || player.getPlaybackState() == ExoPlayer.STATE_IDLE || player
        .getPlaybackState() == ExoPlayer.STATE_ENDED;
  }

  @Override
  public long getCurrentPosition() {
    return player == null ? 0 : player.getCurrentPosition();
  }

  @Override
  public long getDuration() {
    return player == null ? 0 : player.getDuration();
  }

}
