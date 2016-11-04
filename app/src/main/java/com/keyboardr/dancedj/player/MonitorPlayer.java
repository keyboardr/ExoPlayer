package com.keyboardr.dancedj.player;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.keyboardr.dancedj.model.MediaItem;

/**
 * A wrapper around the media player for Monitor playback.
 */

public class MonitorPlayer extends Player {

    private static final String TAG = "MonitorPlayer";

    public MonitorPlayer(@NonNull Context context) {
        super(context);
    }


    public void play(MediaItem mediaItem, boolean playWhenReady) {
        prepareMedia(mediaItem).setPlayWhenReady(playWhenReady);
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

    @SuppressWarnings("unused")
    public void stop() {
        ensurePlayer().stop();
    }

    public void seekTo(long positionMs) {
        ensurePlayer().seekTo(positionMs);
    }

}
