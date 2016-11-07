package com.keyboardr.dancedj.service;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
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
    @IntDef({ClientMessage.TRACK_ADDED, ClientMessage.INDEX_CHANGED, ClientMessage.SET_MEDIA_LIST,
            ClientMessage.SET_OUTPUT_ID, ClientMessage.SET_PLAYBACK_STATE})
    @interface ClientMessage {
        int TRACK_ADDED = 1;
        int INDEX_CHANGED = 2;
        int SET_MEDIA_LIST = 3;
        int SET_OUTPUT_ID = 4;
        int SET_PLAYBACK_STATE = 5;
    }

    static class PlaybackState implements Parcelable {
        private final long lastPosition;
        private final long lastPositionTime;
        private final long duration;
        @PlayState
        private final int playState;
        private final boolean continuePlayingOnDone;

        PlaybackState(long lastPosition, long duration, int playState, boolean continuePlayingOnDone) {
            this.lastPosition = lastPosition;
            this.lastPositionTime = SystemClock.elapsedRealtime();
            this.duration = duration;
            this.playState = playState;
            this.continuePlayingOnDone = continuePlayingOnDone;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.lastPosition);
            dest.writeLong(this.lastPositionTime);
            dest.writeLong(this.duration);
            dest.writeInt(this.playState);
            dest.writeByte(this.continuePlayingOnDone ? (byte) 1 : (byte) 0);
        }

        protected PlaybackState(Parcel in) {
            this.lastPosition = in.readLong();
            this.lastPositionTime = in.readLong();
            this.duration = in.readLong();
            //noinspection WrongConstant
            this.playState = in.readInt();
            this.continuePlayingOnDone = in.readByte() != 0;
        }

        public static final Parcelable.Creator<PlaybackState> CREATOR = new Parcelable.Creator<PlaybackState>() {
            @Override
            public PlaybackState createFromParcel(Parcel source) {
                return new PlaybackState(source);
            }

            @Override
            public PlaybackState[] newArray(int size) {
                return new PlaybackState[size];
            }
        };
    }

    static final String DATA_MEDIA_LIST = "mediaList";
    static final String DATA_PLAYBACK_STATE = "playbackState";

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
                case ClientMessage.SET_PLAYBACK_STATE:
                    //noinspection ConstantConditions
                    playbackState = msg.getData().getParcelable(DATA_PLAYBACK_STATE);
                    if (playbackListener != null) {
                        playbackListener.onPlayStateChanged(PlaylistServiceClient.this);
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
                case ClientMessage.SET_OUTPUT_ID:
                    outputId = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger messenger = new Messenger(new ClientHandler());
    private final Messenger service;

    private List<PlaylistPlayer.PlaylistItem> mediaList = null;
    private int index;
    private int outputId;
    @NonNull
    private PlaybackState playbackState = new PlaybackState(0, 0, PlayState.UNKNOWN, false);

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
        return playbackState.lastPosition +
                (SystemClock.elapsedRealtime() - playbackState.lastPositionTime);
    }

    @Override
    public long getDuration() {
        return playbackState.duration;
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        if (mediaList == null || mediaList.isEmpty() || index >= mediaList.size()) {
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
        return playbackState.playState;
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
        return playbackState.continuePlayingOnDone;
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
