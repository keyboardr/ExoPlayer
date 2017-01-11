package com.keyboardr.bluejay.service;

import android.media.AudioDeviceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.player.PlaylistPlayer;

import java.util.Collections;
import java.util.List;


/**
 * A client object for communicating with a {@link PlaylistMediaService}
 */
public abstract class PlaylistServiceClient implements Player, PlaylistPlayer
    .PlaylistChangedListener {

  @NonNull
  public static MediaItem mediaItemFromQueueItem(@NonNull MediaSessionCompat.QueueItem queueItem) {
    MediaDescriptionCompat description = queueItem.getDescription();
    Bundle extras = description.getExtras();
    if (extras == null) {
      throw new IllegalArgumentException("No extras to get");
    }
    Uri mediaUri = description.getMediaUri();
    return MediaItem.build().setArtist(description.getSubtitle())
        .setAlbumId(extras.getLong(PlaylistMediaService.EXTRA_ALBUM_ID))
        .setTitle(description.getTitle())
        .setPath(mediaUri == null ? null : mediaUri.getPath())
        .setDuration(description.getExtras().getLong(PlaylistMediaService.EXTRA_DURATION))
        .make(extras.getLong(PlaylistMediaService.EXTRA_MEDIA_ID),
            description.getIconUri());
  }

  @NonNull
  private final MediaControllerCompat mediaController;
  @Nullable
  protected PlaybackListener playbackListener;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {

    @Override
    public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
      PlaylistServiceClient.this.onQueueChanged();
    }

    @Override
    public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
      // TODO: 1/10/2017
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
        playbackListener.onSeekComplete(PlaylistServiceClient.this);
      }
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
        playbackListener.onSeekComplete(PlaylistServiceClient.this);
      }
    }

    @Override
    public void onSessionEvent(String event, Bundle extras) {
      // TODO: 1/10/2017
    }

    @Override
    public void onSessionDestroyed() {
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
      }
    }
  };

  public PlaylistServiceClient(@NonNull MediaControllerCompat mediaController) {
    this.mediaController = mediaController;
    mediaController.registerCallback(callback);
  }

  @Override
  public void release() {
    mediaController.unregisterCallback(callback);
  }

  @Override
  public void setAudioOutput(@Nullable AudioDeviceInfo deviceInfo) {
    Bundle extras = new Bundle();
    extras.putInt(PlaylistMediaService.EXTRA_OUTPUT_ID,
        deviceInfo != null ? deviceInfo.getId() : -1);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_SET_OUTPUT, extras, null);
  }

  @Override
  public long getCurrentPosition() {
    return mediaController.getPlaybackState().getPosition();
  }

  @Override
  public long getDuration() {
    return mediaController.getPlaybackState().getBufferedPosition();
  }

  @Override
  public MediaItem getCurrentMediaItem() {
    Bundle extras = mediaController.getPlaybackState().getExtras();
    if (extras == null) {
      return null;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getParcelable(PlaylistMediaService.EXTRA_MEDIA_ITEM);
  }

  @Override
  @PlayState
  public int getPlayState() {
    int state = mediaController.getPlaybackState().getState();
    if (state == PlaybackStateCompat.STATE_PLAYING) {
      return PlayState.PLAYING;
    } else if (state == PlaybackStateCompat.STATE_PAUSED) {
      return PlayState.PAUSED;
    } else if (state == PlaybackStateCompat.STATE_BUFFERING) {
      return PlayState.LOADING;
    } else if (state == PlaybackStateCompat.STATE_STOPPED) {
      return PlayState.STOPPED;
    } else {
      return PlayState.UNKNOWN;
    }
  }

  @Override
  public boolean isPlaying() {
    return getPlayState() == PlayState.PLAYING;
  }

  @Override
  public boolean isPaused() {
    return getPlayState() == PlayState.PAUSED;
  }

  @Override
  public boolean isLoading() {
    return getPlayState() == PlayState.LOADING;
  }

  @Override
  public boolean isStopped() {
    return getPlayState() == PlayState.STOPPED || getPlayState() == PlayState.UNKNOWN;
  }

  public boolean willContinuePlayingOnDone() {
    Bundle extras = mediaController.getPlaybackState().getExtras();
    if (extras == null) {
      return false;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getBoolean(PlaylistMediaService.EXTRA_CONTINUE_ON_DONE);
  }

  @Override
  public int getAudioOutputId() {
    Bundle extras = mediaController.getExtras();
    if (extras == null) {
      return -1;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getInt(PlaylistMediaService.EXTRA_OUTPUT_ID, -1);
  }

  @Override
  public void togglePlayPause() {
    if (willContinuePlayingOnDone()) {
      pause();
    } else {
      resume();
    }
  }

  @Override
  public void resume() {
    mediaController.getTransportControls().play();
  }

  @Override
  public void pause() {
    mediaController.getTransportControls().pause();
  }

  @Override
  public void setPlaybackListener(@Nullable PlaybackListener playbackListener) {
    this.playbackListener = playbackListener;
  }

  public void addToQueue(@NonNull MediaItem mediaItem) {
    Bundle params = new Bundle();
    params.putParcelable(PlaylistMediaService.EXTRA_MEDIA_ITEM, mediaItem);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_ADD_TO_QUEUE, params, null);
  }

  public void moveItem(int oldIndex, int newIndex) {
    Bundle params = new Bundle();
    params.putInt(PlaylistMediaService.EXTRA_INDEX, oldIndex);
    params.putInt(PlaylistMediaService.EXTRA_NEW_INDEX, newIndex);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_MOVE, params, null);
  }

  public void removeItem(int removeIndex) {
    Bundle params = new Bundle();
    params.putInt(PlaylistMediaService.EXTRA_INDEX, removeIndex);
    params.putInt(PlaylistMediaService.EXTRA_NEW_INDEX, -1);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_MOVE, params, null);
  }

  public List<MediaSessionCompat.QueueItem> getQueue() {
    List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
    if (queue == null) {
      return Collections.emptyList();
    }
    return queue;
  }

  public int getCurrentMediaIndex() {
    Bundle extras = mediaController.getPlaybackState().getExtras();
    if (extras == null) {
      return 0;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getInt(PlaylistMediaService.EXTRA_INDEX, 0);
  }
}
