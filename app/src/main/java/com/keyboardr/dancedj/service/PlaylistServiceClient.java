package com.keyboardr.dancedj.service;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.Player;
import com.keyboardr.dancedj.player.PlaylistPlayer;
import com.keyboardr.dancedj.service.PlaylistService.ServiceMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/**
 * A client object for communicating with a {@link PlaylistService}
 */
public abstract class PlaylistServiceClient implements Player, PlaylistPlayer.PlaylistChangedListener {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ClientMessage.TRACK_ADDED, ClientMessage.INDEX_CHANGED, ClientMessage.SET_CURRENT_POSITION,
            ClientMessage.SET_MEDIA_LIST, ClientMessage.SET_PLAY_STATE, ClientMessage.SET_OUTPUT_ID,
            ClientMessage.SET_DURATION})
    @interface ClientMessage {
        int TRACK_ADDED = 1;
        int INDEX_CHANGED = 2;
        int SET_CURRENT_POSITION = 3;
        int SET_MEDIA_LIST = 4;
        int SET_PLAY_STATE = 5;
        int SET_OUTPUT_ID = 6;
        int SET_DURATION = 7;
    }

    static final String DATA_MEDIA_LIST = "mediaList";

    @SuppressLint("HandlerLeak")
    private class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ClientMessage.TRACK_ADDED:
                    onTrackAdded(msg.arg1);
                    break;
                case ClientMessage.INDEX_CHANGED:
                    index = msg.arg2;
                    if (msg.arg1 != msg.arg2) {
                        onIndexChanged(msg.arg1, msg.arg2);
                    }
                    break;
                case ClientMessage.SET_CURRENT_POSITION:
                    lastPosition = (((long) msg.arg1) << 32) | (msg.arg2 & 0xffffffffL);
                    lastPositionTime = SystemClock.elapsedRealtime();
                    if (playbackListener != null) {
                        playbackListener.onSeekComplete(PlaylistServiceClient.this);
                    }
                    break;
                case ClientMessage.SET_MEDIA_LIST:
                    boolean wasNull = mediaList == null;
                    mediaList = msg.getData().getParcelableArrayList(DATA_MEDIA_LIST);
                    if (wasNull) {
                        onMediaListLoaded();
                    }
                    break;
                case ClientMessage.SET_PLAY_STATE:
                    //noinspection WrongConstant
                    playState = msg.arg1;
                    if (playbackListener != null) {
                        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
                    }
                    break;
                case ClientMessage.SET_OUTPUT_ID:
                    outputId = msg.arg1;
                    break;
                case ClientMessage.SET_DURATION:
                    duration = (((long) msg.arg1) << 32) | (msg.arg2 & 0xffffffffL);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger messenger = new Messenger(new ClientHandler());
    private final Messenger service;

    private long lastPosition;
    private long lastPositionTime;
    private long duration;
    private List<PlaylistPlayer.PlaylistItem> mediaList = null;
    private int index;
    @PlayState
    private int playState;
    private int outputId;

    @Nullable
    protected PlaybackListener playbackListener;

    public PlaylistServiceClient(IBinder binder) {
        service = new Messenger(binder);
        Message message = Message.obtain(null, ServiceMessage.REGISTER_CLIENT);
        message.replyTo = messenger;
        try {
            service.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        Message message = Message.obtain(null, ServiceMessage.UNREGISTER_CLIENT);
        message.replyTo = messenger;
        try {
            service.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAudioOutput(@Nullable AudioDeviceInfo deviceInfo) {
        Message message = Message.obtain(null, ServiceMessage.SET_AUDIO_OUTPUT);
        message.arg1 = deviceInfo == null ? -1 : deviceInfo.getId();
        try {
            service.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getCurrentPosition() {
        return lastPosition + SystemClock.elapsedRealtime() - lastPositionTime;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        return mediaList.get(index).mediaItem;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    @PlayState
    public int getPlayState() {
        return playState;
    }

    @Override
    public boolean isPlaying() {
        return playState == PlayState.PLAYING;
    }

    @Override
    public boolean isPaused() {
        return playState == PlayState.PAUSED;
    }

    @Override
    public boolean isLoading() {
        return playState == PlayState.LOADING;
    }

    @Override
    public boolean isStopped() {
        return playState == PlayState.STOPPED || playState == PlayState.UNKNOWN;
    }

    @Override
    public int getAudioOutputId() {
        return outputId;
    }

    @Override
    public void togglePlayPause() {
        try {
            service.send(Message.obtain(null, ServiceMessage.TOGGLE_PLAY_PAUSE));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPlaybackListener(@Nullable PlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
    }

    public void addToQueue(@NonNull MediaItem mediaItem) {
        Message message = Message.obtain(null, ServiceMessage.ADD_TO_QUEUE);
        Bundle data = new Bundle();
        data.putParcelable(PlaylistService.DATA_MEDIA_ITEM, mediaItem);
        message.setData(data);
        try {
            service.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<PlaylistPlayer.PlaylistItem> getMediaList() {
        return mediaList == null ? Collections.<PlaylistPlayer.PlaylistItem>emptyList() : mediaList;
    }

    public int getCurrentMediaIndex() {
        return index;
    }

    public abstract void onMediaListLoaded();
}
