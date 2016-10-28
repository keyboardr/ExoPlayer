package com.keyboardr.dancedj.player;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.keyboardr.dancedj.model.MediaItem;

import java.io.IOException;

/**
 * A wrapper around the media player for Monitor playback.
 */

public class MonitorPlayer {

    public interface PlaybackListener {

        void onSeekComplete(MonitorPlayer player);

        void onPlayStateChanged(MonitorPlayer player);
    }

    private static final String TAG = "MonitorPlayer";
    @NonNull
    private final Context context;
    private final Handler mainHandler;

    @Nullable
    private PlaybackListener playbackListener;

    private final DefaultDataSourceFactory defaultDataSourceFactory;
    private final DefaultExtractorsFactory defaultExtractorsFactory;
    private final ExtractorMediaSource.EventListener extractorListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            Log.e(TAG, "onLoadError: ", error);
        }
    };
    private final ExoPlayer.EventListener playerListener = new ExoPlayer.EventListener() {

        @Override
        public void onLoadingChanged(boolean isLoading) {
            if (playbackListener != null) {
                playbackListener.onPlayStateChanged(MonitorPlayer.this);
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackListener != null) {
                playbackListener.onPlayStateChanged(MonitorPlayer.this);
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            if (playbackListener != null) {
                playbackListener.onSeekComplete(MonitorPlayer.this);
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "onPlayerError: ", error);
        }

        @Override
        public void onPositionDiscontinuity() {
            if (playbackListener != null) {
                playbackListener.onSeekComplete(MonitorPlayer.this);
            }
        }
    };
    @Nullable
    private SimpleExoPlayer player;

    public MonitorPlayer(@NonNull Context context) {
        this.context = context;
        mainHandler = new Handler();
        defaultDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "DanceDJ"));
        defaultExtractorsFactory = new DefaultExtractorsFactory();
        ensurePlayer();
    }

    @NonNull
    private SimpleExoPlayer ensurePlayer() {
        if (player != null) return player;
        player = ExoPlayerFactory.newSimpleInstance(context,
                new DefaultTrackSelector(mainHandler),
                new DefaultLoadControl());
        player.addListener(playerListener);
        return player;
    }

    public void setPlaybackListener(@Nullable PlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
    }

    public void setAudioOutput(@Nullable AudioDeviceInfo audioDeviceInfo) {
        ensurePlayer().setAudioOutput(audioDeviceInfo);
    }

    @Nullable
    public AudioDeviceInfo getAudioOutput() {
        if (player == null) {
            return null;
        }
        return player.getAudioOutput();
    }

    public void play(MediaItem mediaItem, boolean playWhenReady) {
        ExoPlayer player = ensurePlayer();
        player.prepare(new ExtractorMediaSource(mediaItem.toUri(), defaultDataSourceFactory,
                defaultExtractorsFactory, mainHandler, extractorListener));
        player.setPlayWhenReady(playWhenReady);
    }

    public void release() {
        if (player != null) {
            player.removeListener(playerListener);
            player.release();
            player = null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void resume() {
        if (player != null) {
            if (player.getPlaybackState() == ExoPlayer.STATE_ENDED) {
                player.seekTo(0);
            }
            player.setPlayWhenReady(true);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    public void togglePlayPause() {
        if (isPaused() || isStopped()) {
            resume();
        } else {
            pause();
        }
    }

    @SuppressWarnings("unused")
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    public boolean isPlaying() {
        return player != null && player.getPlaybackState() == ExoPlayer.STATE_READY && player.getPlayWhenReady();
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isPaused() {
        return player != null && player.getPlaybackState() == ExoPlayer.STATE_READY && !player.getPlayWhenReady();
    }

    @SuppressWarnings("unused")
    public boolean isLoading() {
        return player != null && (player.isLoading() || player.getPlaybackState() == ExoPlayer.STATE_BUFFERING);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isStopped() {
        return player == null || player.getPlaybackState() == ExoPlayer.STATE_IDLE || player.getPlaybackState() == ExoPlayer.STATE_ENDED;
    }

    public long getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    public long getDuration() {
        return player == null ? 0 : player.getDuration();
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

}
