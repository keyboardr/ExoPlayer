package com.keyboardr.dancedj.player;

import android.content.Context;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.keyboardr.dancedj.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPlayer extends AbsPlayer {

    public interface PlaylistChangedListener {
        void onTrackAdded(int index);

        void onIndexChanged(int oldIndex, int newIndex);
    }

    private List<PlaylistItem> mediaItems = new ArrayList<>();
    private int currentIndex;
    private int nextId;
    @Nullable
    private PlaylistChangedListener playlistChangedListener;

    public PlaylistPlayer(@NonNull Context context) {
        super(context, AudioManager.STREAM_MUSIC);
        ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_ENDED && playWhenReady) {
                    currentIndex++;
                    if (mediaItems.size() > currentIndex) {
                        ensurePlayer().prepare(getMediaSource(mediaItems.get(currentIndex).mediaItem));
                    }
                    if (playlistChangedListener != null) {
                        playlistChangedListener.onIndexChanged(currentIndex - 1, currentIndex);
                    }
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
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

    public void addPlaylistChangedListener(@Nullable PlaylistChangedListener playlistChangedListener) {
        this.playlistChangedListener = playlistChangedListener;
    }

    public void addToQueue(@NonNull MediaItem mediaItem) {
        mediaItems.add(new PlaylistItem(mediaItem, nextId++));
        if (ensurePlayer().getPlaybackState() == ExoPlayer.STATE_IDLE) {
            ensurePlayer().prepare(getMediaSource(mediaItems.get(0).mediaItem));
        }
        if (playlistChangedListener != null) {
            playlistChangedListener.onTrackAdded(mediaItems.size() - 1);
        }
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        if (mediaItems.isEmpty()) {
            return null;
        }
        return mediaItems.get(getCurrentMediaIndex()).mediaItem;
    }

    public int getCurrentMediaIndex() {
        return currentIndex;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public void togglePlayPause() {
        ensurePlayer().setPlayWhenReady(true);
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

        public static final Parcelable.Creator<PlaylistItem> CREATOR = new Parcelable.Creator<PlaylistItem>() {
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
}
