package com.keyboardr.bluejay.service;

import android.media.AudioDeviceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.PlaybackFinishEvent;
import com.keyboardr.bluejay.bus.event.QueueChangeEvent;
import com.keyboardr.bluejay.bus.event.SetMetadataEvent;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.bus.event.VolumeEvent;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.player.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static android.content.ContentValues.TAG;


/**
 * A client object for communicating with a {@link PlaylistMediaService}
 */
public class PlaylistServiceClient implements Player {

  @NonNull
  public static MediaItem mediaItemFromQueueItem(@NonNull MediaSessionCompat.QueueItem queueItem) {
    MediaDescriptionCompat description = queueItem.getDescription();
    Bundle extras = description.getExtras();
    if (extras == null) {
      throw new IllegalArgumentException("No extras to get");
    }
    Uri mediaUri = description.getMediaUri();
    return MediaItem.build().setArtist(description.getSubtitle())
        .setAlbumId(extras.getLong(PlaylistMediaService.QUEUE_ALBUM_ID))
        .setTitle(description.getTitle())
        .setPath(mediaUri == null ? null : mediaUri.getPath())
        .setDuration(description.getExtras().getLong(PlaylistMediaService.QUEUE_DURATION))
        .make(extras.getLong(PlaylistMediaService.QUEUE_MEDIA_ID));
  }

  @NonNull
  private final MediaControllerCompat mediaController;
  @Nullable
  protected PlaybackListener playbackListener;

  @Nullable
  private List<MediaSessionCompat.QueueItem> queue;

  private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {

    private int lastKnownIndex = -1;

    @Override
    public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
      boolean notify = false;
      if (PlaylistServiceClient.this.queue == null || PlaylistServiceClient.this.queue.size() !=
          queue.size()) {
        notify = true;
      }
      PlaylistServiceClient.this.queue = queue;
      if (notify) {
        Buses.PLAYLIST.postSticky(QueueChangeEvent.newInstance(queue));
      }
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
      if (playbackListener != null) {
        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
        playbackListener.onSeekComplete(PlaylistServiceClient.this);
      }
      int currentMediaIndex = getCurrentMediaIndex();
      if (currentMediaIndex != lastKnownIndex) {
        Buses.PLAYLIST.postSticky(
            new TrackIndexEvent(lastKnownIndex, currentMediaIndex, getCurrentMediaItem()));
        lastKnownIndex = currentMediaIndex;
      }
      Bundle extras = state.getExtras();
      if (extras == null) {
        return;
      }
      extras.setClassLoader(getClass().getClassLoader());

      if (extras.getLong(PlaylistMediaService.PLAYSTATE_FINISH_TIME, -1) > 0) {
        PlaybackFinishEvent existing = Buses.PLAYLIST.getStickyEvent(PlaybackFinishEvent.class);
        long finishTime = extras.getLong(PlaylistMediaService.PLAYSTATE_FINISH_TIME);
        if (existing == null || existing.finishTime != finishTime) {
          Buses.PLAYLIST.postSticky(new PlaybackFinishEvent(finishTime));
        }
      } else {
        Buses.PLAYLIST.removeStickyEvent(PlaybackFinishEvent.class);
      }

      VolumeEvent existing = Buses.PLAYLIST.getStickyEvent(VolumeEvent.class);
      float volume = extras.getFloat(PlaylistMediaService.PLAYSTATE_VOLUME, 1);
      if (existing == null || existing.volume != volume) {
        Buses.PLAYLIST.postSticky(new VolumeEvent(volume));
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
    public void onExtrasChanged(Bundle extras) {
      super.onExtrasChanged(extras);
      extras.setClassLoader(getClass().getClassLoader());
      SetMetadata oldMetadata = SetMetadataEvent.getSetMetadata(Buses.PLAYLIST);
      SetMetadata extrasMetadata = extras.getParcelable(PlaylistMediaService.EXTRA_SET_METADATA);
      if (!Objects.equals(oldMetadata, extrasMetadata)) {
        Buses.PLAYLIST.postSticky(new SetMetadataEvent(extrasMetadata));
      }
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
    Buses.PLAYLIST.postSticky(
        new TrackIndexEvent(-1, getCurrentMediaIndex(), getCurrentMediaItem()));
  }

  @Override
  public void release() {
    mediaController.unregisterCallback(callback);
    Buses.PLAYLIST.removeAllStickyEvents();
  }

  @Override
  public void setAudioOutput(@Nullable AudioDeviceInfo deviceInfo) {
    Bundle extras = new Bundle();
    extras.putInt(PlaylistMediaService.EXTRA_OUTPUT_TYPE,
        deviceInfo != null ? deviceInfo.getType() : AudioDeviceInfo.TYPE_UNKNOWN);
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
  public void setVolume(@FloatRange(from = 0, to = 1) float volume) {
    Bundle extras = new Bundle();
    extras.putFloat(PlaylistMediaService.EXTRA_NEW_VOLUME, volume);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_SET_VOLUME, extras, null);
  }

  @Override
  public float getVolume() {
    VolumeEvent event = Buses.PLAYLIST.getStickyEvent(VolumeEvent.class);
    return event == null ? 1 : event.volume;
  }

  @Override
  public MediaItem getCurrentMediaItem() {
    Bundle extras = mediaController.getPlaybackState().getExtras();
    if (extras == null) {
      return null;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getParcelable(PlaylistMediaService.PLAYSTATE_MEDIA_ITEM);
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
    return extras.getBoolean(PlaylistMediaService.PLAYSTATE_CONTINUE_ON_DONE);
  }

  @Override
  public int getAudioOutputType() {
    Bundle extras = mediaController.getExtras();
    if (extras == null) {
      return -1;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getInt(PlaylistMediaService.EXTRA_OUTPUT_TYPE, -1);
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
    queue = null;
    Bundle params = new Bundle();
    params.putParcelable(PlaylistMediaService.EXTRA_MEDIA_ITEM, mediaItem);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_ADD_TO_QUEUE, params, null);
  }

  public void moveItem(int oldIndex, int newIndex) {
    Log.d(TAG, "moveItem: ");
    if (queue != null) {
      queue.add(newIndex, queue.remove(oldIndex));
    }
    Bundle params = new Bundle();
    params.putInt(PlaylistMediaService.EXTRA_INDEX, oldIndex);
    params.putInt(PlaylistMediaService.EXTRA_NEW_INDEX, newIndex);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_MOVE, params, null);
  }

  public void removeItem(int removeIndex) {
    queue = null;
    Bundle params = new Bundle();
    params.putInt(PlaylistMediaService.EXTRA_INDEX, removeIndex);
    params.putInt(PlaylistMediaService.EXTRA_NEW_INDEX, -1);
    mediaController.sendCommand(PlaylistMediaService.COMMAND_MOVE, params, null);
  }

  @NonNull
  public List<MediaSessionCompat.QueueItem> getQueue() {
    if (this.queue != null) {
      return this.queue;
    }
    List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
    if (queue == null) {
      return Collections.emptyList();
    }
    this.queue = queue;
    Buses.PLAYLIST.postSticky(QueueChangeEvent.newInstance(queue));
    return queue;
  }

  private boolean queueEqual(List<MediaSessionCompat.QueueItem> left, List<MediaSessionCompat
      .QueueItem> right) {
    if (left.size() != right.size()) {
      return false;
    }
    for (int i = 0; i < left.size(); i++) {
      if (left.get(i).getQueueId() != right.get(i).getQueueId()) {
        return false;
      }
    }
    return true;
  }

  public int getCurrentMediaIndex() {
    Bundle extras = mediaController.getPlaybackState().getExtras();
    if (extras == null) {
      return 0;
    }
    extras.setClassLoader(getClass().getClassLoader());
    return extras.getInt(PlaylistMediaService.PLAYSTATE_INDEX, 0);
  }
}
