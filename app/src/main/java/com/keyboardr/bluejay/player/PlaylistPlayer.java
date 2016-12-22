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
import com.keyboardr.bluejay.model.MediaItem;

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
    private boolean continuePlayingOnDone;

    @Nullable
    private PlaylistChangedListener playlistChangedListener;

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
                        player.prepare(getMediaSource(mediaItems.get(currentIndex).mediaItem));
                    } else {
                        // End of queue. Get ready for more tracks to be added.
                        continuePlayingOnDone = false;
                        player.setPlayWhenReady(false);
                    }
                    if (playlistChangedListener != null) {
                        playlistChangedListener.onIndexChanged(currentIndex - 1, currentIndex);
                    }
                    if (playbackListener != null) {
                        playbackListener.onPlayStateChanged(PlaylistPlayer.this);
                    }
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
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
        SimpleExoPlayer player = ensurePlayer();
        if (player.getPlaybackState() == ExoPlayer.STATE_IDLE
                || player.getPlaybackState() == ExoPlayer.STATE_ENDED) {
            player.prepare(getMediaSource(mediaItems.get(currentIndex).mediaItem));
        }
        if (playlistChangedListener != null) {
            playlistChangedListener.onTrackAdded(mediaItems.size() - 1);
        }
        if (playbackListener != null) {
            playbackListener.onPlayStateChanged(this);
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
    public void togglePlayPause() {
        continuePlayingOnDone = !continuePlayingOnDone;
        if (continuePlayingOnDone) {
            ensurePlayer().setPlayWhenReady(true);
        } else {
            Toast.makeText(context, "Playback will stop at the end of the current track",
                    Toast.LENGTH_LONG).show();
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
