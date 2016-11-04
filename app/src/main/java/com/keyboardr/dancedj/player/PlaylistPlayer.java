package com.keyboardr.dancedj.player;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.keyboardr.dancedj.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPlayer extends Player {

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
        if (mediaItems.size() == 0) {
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

    public List<PlaylistItem> getMediaList() {
        return new ArrayList<>(mediaItems);
    }

    public static class PlaylistItem {
        public final MediaItem mediaItem;
        public final long id;

        public PlaylistItem(MediaItem mediaItem, long id) {
            this.mediaItem = mediaItem;
            this.id = id;
        }
    }
}
