package com.keyboardr.bluejay.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.player.PlaylistPlayer;
import com.keyboardr.bluejay.service.PlaylistServiceClient.ClientMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class PlaylistService extends Service implements PlaylistPlayer.PlaylistChangedListener,
    Player.PlaybackListener {

  public static final String EXTRA_END_SET = "endSet";
  static final String DATA_MEDIA_ITEM = "mediaItem";
  static final String DATA_INDEX = "index";
  static final String DATA_NEW_INDEX = "newIndex";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({ServiceMessage.REGISTER_CLIENT, ServiceMessage.UNREGISTER_CLIENT,
      ServiceMessage.SET_AUDIO_OUTPUT, ServiceMessage.TOGGLE_PLAY_PAUSE,
      ServiceMessage.ADD_TO_QUEUE, ServiceMessage.REMOVE_ITEM, ServiceMessage.MOVE_ITEM})
  @interface ServiceMessage {
    int REGISTER_CLIENT = 1;
    int UNREGISTER_CLIENT = 2;
    int SET_AUDIO_OUTPUT = 3;
    int TOGGLE_PLAY_PAUSE = 4;
    int ADD_TO_QUEUE = 5;
    int REMOVE_ITEM = 6;
    int MOVE_ITEM = 7;
  }

  private static final String TAG = "PlaylistService";

  private final Messenger messenger = new Messenger(new IncomingHandler());
  private final List<Messenger> clients = new ArrayList<>();

  private PlaylistPlayer player;

  @Override
  public void onCreate() {
    super.onCreate();
    player = new PlaylistPlayer(this);
    player.setPlaybackListener(this);
    player.addPlaylistChangedListener(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    if (intent.getBooleanExtra(EXTRA_END_SET, false)) {
      stopForeground(true);
      stopSelf();
      return START_REDELIVER_INTENT;
    } else {
      startForeground(1, getNotification());
    }
    return START_STICKY;
  }

  private Notification getNotification() {
    return new Notification.Builder(this).build();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    player.release();
    player = null;
  }

  private void sendMessageToClients(@ClientMessage int what, int arg1) {
    sendMessageToClients(what, arg1, 0, null);
  }

  private void sendMessageToClients(@ClientMessage int what, int arg1, int arg2) {
    sendMessageToClients(what, arg1, arg2, null);
  }

  private void sendMessageToClients(@ClientMessage int what, int arg1, int arg2, @Nullable Bundle
      data) {
    for (int i = clients.size() - 1; i >= 0; i--) {
      Messenger client = clients.get(i);
      try {
        Message message = Message.obtain(null, what, arg1, arg2);
        if (data != null) {
          message.setData(data);
        }
        client.send(message);
      } catch (RemoteException e) {
        // The client is dead. Remove it. We're going through the list backwards, so this is ok
        clients.remove(i);
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onTrackAdded(int index) {
    Bundle data = new Bundle();
    data.putParcelableArrayList(PlaylistServiceClient.DATA_MEDIA_LIST, player.getMediaList());
    sendMessageToClients(ClientMessage.SET_MEDIA_LIST, 0, 0, data);
    sendMessageToClients(ClientMessage.TRACK_ADDED, index);
  }

  @Override
  public void onIndexChanged(int oldIndex, int newIndex) {
    sendMessageToClients(ClientMessage.INDEX_CHANGED, oldIndex, newIndex);
    sendPlaybackState();
  }

  @SuppressLint("HandlerLeak")
  private class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      msg.getData().setClassLoader(getClassLoader());
      switch (msg.what) {
        case ServiceMessage.REGISTER_CLIENT:
          clients.add(msg.replyTo);
          sendInitialInfo(msg.replyTo);
          return;
        case ServiceMessage.UNREGISTER_CLIENT:
          clients.remove(msg.replyTo);
          return;
        case ServiceMessage.SET_AUDIO_OUTPUT:
          setPlayerOutput(msg.arg1);
          sendMessageToClients(ClientMessage.SET_OUTPUT_ID, player.getAudioOutputId());
          return;
        case ServiceMessage.TOGGLE_PLAY_PAUSE:
          player.togglePlayPause();
          sendPlaybackState();
          return;
        case ServiceMessage.ADD_TO_QUEUE:
          //noinspection ConstantConditions
          player.addToQueue(((MediaItem) msg.getData().getParcelable(DATA_MEDIA_ITEM)));
          break;
        case ServiceMessage.REMOVE_ITEM:
          player.removeItem(msg.getData().getInt(DATA_INDEX));
          break;
        case ServiceMessage.MOVE_ITEM:
          player.moveItem(msg.getData().getInt(DATA_INDEX), msg.getData().getInt(DATA_NEW_INDEX));
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }

  private PlaylistServiceClient.PlaybackState createPlaybackState() {
    return new PlaylistServiceClient.PlaybackState(player.getCurrentPosition(),
        player.getDuration(), player.getPlayState(), player.willContinuePlayingOnDone());
  }

  private void sendInitialInfo(Messenger client) {
    try {
      client.send(getSetMediaListMessage());

      client.send(Message.obtain(null, ClientMessage.INDEX_CHANGED,
          player.getCurrentMediaIndex(), player.getCurrentMediaIndex()));

      sendPlaybackState();

      client.send(Message.obtain(null, ClientMessage.SET_OUTPUT_ID, player.getAudioOutputId(), 0));

    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  private void sendPlaybackState() {
    Bundle playbackData = new Bundle();
    playbackData.putParcelable(PlaylistServiceClient.DATA_PLAYBACK_STATE, createPlaybackState());
    sendMessageToClients(ClientMessage.SET_PLAYBACK_STATE, 0, 0, playbackData);
  }

  @NonNull
  private Message getSetMediaListMessage() {
    Message mediaList = Message.obtain(null, ClientMessage.SET_MEDIA_LIST);
    mediaList.getData().putParcelableArrayList(PlaylistServiceClient.DATA_MEDIA_LIST,
        player.getMediaList());
    return mediaList;
  }


  @Override
  public void onSeekComplete(Player player) {
  }

  @Override
  public void onPlayStateChanged(Player player) {
    sendPlaybackState();
  }

  private void setPlayerOutput(int outputId) {
    if (outputId == -1) {
      player.setAudioOutput(null);
      return;
    }
    AudioDeviceInfo[] devices = getSystemService(AudioManager.class).getDevices(AudioManager
        .GET_DEVICES_OUTPUTS);
    for (AudioDeviceInfo deviceInfo : devices) {
      if (deviceInfo.getId() == outputId) {
        player.setAudioOutput(deviceInfo);
        return;
      }
    }
    Log.w(TAG, "setPlayerOutput() output not found: " + outputId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    startService(new Intent(this, PlaylistService.class));
    return messenger.getBinder();
  }
}
