package com.keyboardr.dancedj.player;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.keyboardr.dancedj.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPlayer extends Player {

    private List<MediaItem> mediaItems = new ArrayList<>();
    private int currentIndex;

    public PlaylistPlayer(@NonNull Context context) {
        super(context);
        ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_ENDED && playWhenReady) {
                    currentIndex++;
                    if (mediaItems.size() > currentIndex) {
                        ensurePlayer().prepare(getMediaSource(mediaItems.get(currentIndex)));
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

    public void addToQueue(@NonNull MediaItem mediaItem) {
        mediaItems.add(mediaItem);
        if (ensurePlayer().getPlaybackState() == ExoPlayer.STATE_IDLE) {
            ensurePlayer().prepare(getMediaSource(mediaItems.get(0)));
        }
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        if (mediaItems.size() == 0) {
            return null;
        }
        return mediaItems.get(getCurrentMediaIndex());
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
}
