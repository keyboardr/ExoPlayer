package com.keyboardr.bluejay.player;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.keyboardr.bluejay.BuildConfig;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.model.MediaItem;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPlayer extends AbsPlayer {

  private static final boolean DEBUG_SHORT_SONGS = true;

  public interface QueueChangedListener {
    void onQueueChanged();
  }

  private List<PlaylistItem> mediaItems = new ArrayList<>();
  private int currentIndex;
  private int nextId;
  private boolean continuePlayingOnDone;

  @Nullable
  private QueueChangedListener queueChangedListener;

  public PlaylistPlayer(@NonNull Context context) {
    super(context, C.STREAM_TYPE_ALARM);
    ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
      @Override
      public void onLoadingChanged(boolean isLoading) {
      }

      @Override
      public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED && playWhenReady) {
          currentIndex++;
          SimpleExoPlayer player = ensurePlayer();
          if (mediaItems.size() > currentIndex) {
            // More tracks in the queue. Continue iff continuePlayingOnDone is set.
            player.setPlayWhenReady(continuePlayingOnDone);
            prepareNextTrack(player);
          } else {
            // End of queue. Get ready for more tracks to be added.
            continuePlayingOnDone = false;
            player.setPlayWhenReady(false);
          }
          getBus().postSticky(new TrackIndexEvent(currentIndex - 1, currentIndex));
          if (playbackListener != null) {
            playbackListener.onPlayStateChanged(PlaylistPlayer.this);
          }
        }
      }

      @Override
      public void onTimelineChanged(Timeline timeline, Object manifest) {
      }

      @Override
      public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray
          trackSelections) {
      }

      @Override
      public void onPlayerError(ExoPlaybackException error) {
      }

      @Override
      public void onPositionDiscontinuity() {
      }
    };
    ensurePlayer().addListener(eventListener);
  }

  private void prepareNextTrack(SimpleExoPlayer player) {
    MediaItem mediaItem = mediaItems.get(currentIndex).mediaItem;
    player.prepare(getMediaSource(mediaItem));
    if (BuildConfig.DEBUG && DEBUG_SHORT_SONGS) {
      player.seekTo(mediaItem.getDuration() - 10000);
    }
  }

  public void addPlaylistChangedListener(@Nullable QueueChangedListener
                                             queueChangedListener) {
    this.queueChangedListener = queueChangedListener;
  }

  public void addToQueue(@NonNull MediaItem mediaItem) {
    mediaItems.add(new PlaylistItem(mediaItem, nextId++));
    SimpleExoPlayer player = ensurePlayer();
    if (player.getPlaybackState() == ExoPlayer.STATE_IDLE
        || player.getPlaybackState() == ExoPlayer.STATE_ENDED) {
      prepareNextTrack(player);
    }
    if (queueChangedListener != null) {
      queueChangedListener.onQueueChanged();
    }
  }

  public void removeItem(int removeIndex) {
    mediaItems.remove(removeIndex);
    if (queueChangedListener != null) {
      queueChangedListener.onQueueChanged();
    }
  }

  public void moveItem(int oldIndex, int newIndex) {
    mediaItems.add(newIndex, mediaItems.remove(oldIndex));
    if (queueChangedListener != null) {
      queueChangedListener.onQueueChanged();
    }

  }

  @Override
  public MediaItem getCurrentMediaItem() {
    if (mediaItems.isEmpty()) {
      return null;
    }
    int currentMediaIndex = getCurrentMediaIndex();
    if (currentMediaIndex >= mediaItems.size()) {
      return null;
    }
    return mediaItems.get(currentMediaIndex).mediaItem;
  }

  public int getCurrentMediaIndex() {
    return currentIndex;
  }

  @Override
  public void togglePlayPause() {
    if (continuePlayingOnDone) {
      pause();
    } else {
      resume();
    }
  }

  @Override
  public void resume() {
    continuePlayingOnDone = true;
    ensurePlayer().setPlayWhenReady(true);
    if (playbackListener != null) {
      playbackListener.onPlayStateChanged(this);
    }
  }

  @Override
  public void pause() {
    continuePlayingOnDone = false;
    Toast.makeText(context, "Playback will stop at the end of the current track",
        Toast.LENGTH_LONG).show();
    if (playbackListener != null) {
      playbackListener.onPlayStateChanged(this);
    }
  }

  public boolean willContinuePlayingOnDone() {
    return continuePlayingOnDone;
  }

  public ArrayList<PlaylistItem> getMediaList() {
    return new ArrayList<>(mediaItems);
  }

  public static class PlaylistItem implements Parcelable {
    public final MediaItem mediaItem;
    public final long id;

    public PlaylistItem(MediaItem mediaItem, long id) {
      this.mediaItem = mediaItem;
      this.id = id;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(this.mediaItem, flags);
      dest.writeLong(this.id);
    }

    protected PlaylistItem(Parcel in) {
      this.mediaItem = in.readParcelable(MediaItem.class.getClassLoader());
      this.id = in.readLong();
    }

    public static final Parcelable.Creator<PlaylistItem> CREATOR = new Parcelable
        .Creator<PlaylistItem>() {
      @Override
      public PlaylistItem createFromParcel(Parcel source) {
        return new PlaylistItem(source);
      }

      @Override
      public PlaylistItem[] newArray(int size) {
        return new PlaylistItem[size];
      }
    };
  }

  @Override
  protected EventBus getBus() {
    return Buses.PLAYLIST;
  }
}
