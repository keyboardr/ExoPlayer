package com.keyboardr.bluejay.player;

import android.media.AudioDeviceInfo;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.keyboardr.bluejay.model.MediaItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * General interface for interacting with players
 */
public interface Player {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PlayState.UNKNOWN, PlayState.PLAYING, PlayState.PAUSED, PlayState.LOADING, PlayState.STOPPED})
    @interface PlayState {
        int UNKNOWN = 0;
        int PLAYING = 1;
        int PAUSED = 2;
        int LOADING = 3;
        int STOPPED = 4;
    }

    interface PlaybackListener {

        void onSeekComplete(Player player);

        void onPlayStateChanged(Player player);

        void onVolumeChanged(Player player);
    }

    void setPlaybackListener(@Nullable PlaybackListener playbackListener);

    void setAudioOutput(@Nullable AudioDeviceInfo audioDeviceInfo);

    int getAudioOutputType();

    @PlayState
    int getPlayState();

    void release();

    void togglePlayPause();

    void resume();

    void pause();

    @Nullable
    MediaItem getCurrentMediaItem();

    boolean isPlaying();

    @SuppressWarnings("WeakerAccess")
    boolean isPaused();

    @SuppressWarnings("unused")
    boolean isLoading();

    @SuppressWarnings("WeakerAccess")
    boolean isStopped();

    long getCurrentPosition();

    long getDuration();

    void setVolume(@FloatRange(from = 0, to = 1) float volume);

    @FloatRange(from = 0, to = 1)
    float getVolume();
}
