package com.keyboardr.bluejay.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.keyboardr.bluejay.PlaybackActivity;
import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.player.PlaylistPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlaylistService using MediaBrowserService
 */

public class PlaylistMediaService extends MediaBrowserServiceCompat
    implements Player.PlaybackListener, PlaylistPlayer.PlaylistChangedListener {

  public static final String ACTION_CHECK_IS_ALIVE = "checkIsAlive";

  static final String COMMAND_SET_OUTPUT = "setOutput";
  static final String COMMAND_ADD_TO_QUEUE = "addToQueue";
  static final String COMMAND_MOVE = "move";
  static final String EXTRA_MEDIA_ITEM = "PlaylistMediaService.mediaItem";
  static final String EXTRA_OUTPUT_ID = "outputId";
  static final String EXTRA_INDEX = "index";
  static final String EXTRA_NEW_INDEX = "newIndex";
  static final String EXTRA_CONTINUE_ON_DONE = "continueOnDone";
  static final String EXTRA_MEDIA_ID = "mediaId";
  static final String EXTRA_ALBUM_ID = "albumId";
  static final String EXTRA_DURATION = "duration";

  private static final String TAG = "PlaylistMediaService";

  private final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
  private final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
  private MediaItem metadataMedia;

  private final MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
    @Override
    public void onPause() {
      player.pause();
    }

    @Override
    public void onPlay() {
      player.resume();
    }

    @Override
    public void onStop() {
      stopSelf();
      stopForeground(true);
    }

    @Override
    public void onCommand(String command, Bundle extras, ResultReceiver cb) {
      extras.setClassLoader(getClassLoader());
      switch (command) {
        case COMMAND_SET_OUTPUT:
          int outputId = extras.getInt(EXTRA_OUTPUT_ID);
          if (outputId == -1) {
            player.setAudioOutput(null);
            updateOutputId();
            return;
          }
          AudioDeviceInfo[] devices = getSystemService(AudioManager.class).getDevices(AudioManager
              .GET_DEVICES_OUTPUTS);
          for (AudioDeviceInfo deviceInfo : devices) {
            if (deviceInfo.getId() == outputId) {
              player.setAudioOutput(deviceInfo);
              updateOutputId();
              return;
            }
          }
          Log.w(TAG, "setPlayerOutput() output not found: " + outputId);
          break;
        case COMMAND_ADD_TO_QUEUE:
          MediaItem mediaItem = extras.getParcelable(EXTRA_MEDIA_ITEM);
          if (mediaItem != null) {
            player.addToQueue(mediaItem);
          }
          break;
        case COMMAND_MOVE:
          int oldIndex = extras.getInt(EXTRA_INDEX);
          int newIndex = extras.getInt(EXTRA_NEW_INDEX, -1);

          if (newIndex == -1) {
            player.removeItem(oldIndex);
          } else {
            player.moveItem(oldIndex, newIndex);
          }
      }
    }

  };

  private MediaSessionCompat mediaSession;
  private PlaylistPlayer player;
  private BroadcastReceiver isAliveReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      setResultCode(Activity.RESULT_OK);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();

    stateBuilder.addCustomAction(COMMAND_ADD_TO_QUEUE, COMMAND_ADD_TO_QUEUE,
        R.drawable.ic_playlist_add);
    stateBuilder.addCustomAction(COMMAND_MOVE, COMMAND_MOVE, R.drawable.ic_drag_handle);
    stateBuilder.addCustomAction(COMMAND_SET_OUTPUT, COMMAND_SET_OUTPUT,
        R.drawable.ic_more_vert_black);

    mediaSession = new MediaSessionCompat(this, TAG);
    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
    mediaSession.setCallback(callback);
    mediaSession.setActive(true);
    mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0, new Intent(this,
        PlaybackActivity.class), 0));

    player = new PlaylistPlayer(this);
    player.setPlaybackListener(this);
    player.addPlaylistChangedListener(this);

    setSessionToken(mediaSession.getSessionToken());

    updateOutputId();
    updatePlaybackState();
    updateMetadata();

    registerReceiver(isAliveReceiver, new IntentFilter(ACTION_CHECK_IS_ALIVE));
    startService(new Intent(this, PlaylistMediaService.class));
  }

  private Notification getNotification() {
    MediaControllerCompat controller = mediaSession.getController();
    MediaMetadataCompat metadata = controller.getMetadata();

    CharSequence title;
    CharSequence subtitle;
    Bitmap iconBitmap;
    if (metadata == null) {
      title = getString(R.string.no_media_playing);
      subtitle = "";
      iconBitmap = null;
    } else {
      MediaDescriptionCompat description = metadata.getDescription();
      title = description.getTitle();
      subtitle = description.getSubtitle();
      iconBitmap = description.getIconBitmap();
    }
    return new NotificationCompat.Builder(this)
        .setContentTitle(title)
        .setContentText(subtitle)
        .setLargeIcon(iconBitmap)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_play_arrow) // TODO: 1/10/2017 Update this icon when I have one
        .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        .setContentIntent(controller.getSessionActivity())
        .setStyle(new NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.getSessionToken())
            .setShowCancelButton(false))
        .build();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mediaSession.setActive(false);
    mediaSession.release();
    mediaSession = null;
    metadataMedia = null;

    player.release();
    player = null;
    unregisterReceiver(isAliveReceiver);
  }

  @Nullable
  @Override
  public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                               @Nullable Bundle rootHints) {
    if (getApplication().getPackageName().equals(clientPackageName)) {
      return new BrowserRoot("", null);
    }
    return null;
  }

  @Override
  public void onLoadChildren(@NonNull String parentId,
                             @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
    result.sendResult(null);
  }

  private void updateOutputId() {
    Bundle extras = mediaSession.getController().getExtras();
    if (extras == null) {
      extras = new Bundle();
    }
    extras.putInt(EXTRA_OUTPUT_ID, player.getAudioOutputId());
    mediaSession.setExtras(extras);
  }

  private void updatePlaybackState() {
    @PlaybackStateCompat.State int playerState;
    @PlaybackStateCompat.Actions long actions;
    if (player.isStopped()) {
      playerState = PlaybackStateCompat.STATE_STOPPED;
      if (player.getCurrentMediaIndex() < player.getMediaList().size()) {
        actions = PlaybackStateCompat.ACTION_PLAY;
      } else {
        actions = 0;
      }
    } else if (player.isPaused()) {
      playerState = PlaybackStateCompat.STATE_PAUSED;
      actions = PlaybackStateCompat.ACTION_PLAY;
    } else if (player.isLoading()) {
      playerState = PlaybackStateCompat.STATE_BUFFERING;
      actions = 0;
    } else if (this.player.willContinuePlayingOnDone()) {
      playerState = PlaybackStateCompat.STATE_PLAYING;
      actions = PlaybackStateCompat.ACTION_PAUSE;
    } else {
      playerState = PlaybackStateCompat.STATE_PLAYING;
      actions = PlaybackStateCompat.ACTION_PLAY_PAUSE;
    }

    actions |= PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID;
    stateBuilder.setState(playerState, this.player.getCurrentPosition(), 1.0f);
    stateBuilder.setBufferedPosition(this.player.getDuration());
    stateBuilder.setActions(actions);
    MediaItem currentMediaItem = player.getCurrentMediaItem();
    stateBuilder.setActiveQueueItemId(currentMediaItem == null ? 0 : currentMediaItem
        .getTransientId());
    Bundle extras = new Bundle();
    extras.putParcelable(EXTRA_MEDIA_ITEM, currentMediaItem);
    extras.putBoolean(EXTRA_CONTINUE_ON_DONE, player.willContinuePlayingOnDone());
    extras.putInt(EXTRA_INDEX, player.getCurrentMediaIndex());
    stateBuilder.setExtras(extras);
    mediaSession.setPlaybackState(stateBuilder.build());

  }

  private void updateMetadata() {
    MediaItem mediaItem = player.getCurrentMediaItem();
    if (!Objects.equals(mediaItem, metadataMedia)) {
      metadataMedia = mediaItem;
      if (mediaItem == null) {
        mediaSession.setMetadata(null);
      } else {

        MediaMetadataCompat metadata = metadataBuilder
            .putText(MediaMetadataCompat.METADATA_KEY_TITLE, mediaItem.title)
            .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaItem.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                mediaItem.thumbnailUri != null ? mediaItem.thumbnailUri
                    .toString() : null)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaItem.toUri().toString())
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                String.valueOf(mediaItem.getTransientId()))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaItem.getDuration())
            .build();
        mediaSession.setMetadata(metadata);
      }
    }
    startForeground(1, getNotification());
  }

  private List<MediaSessionCompat.QueueItem> buildQueue() {
    List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
    for (PlaylistPlayer.PlaylistItem playlistItem : player.getMediaList()) {
      Bundle extras = new Bundle();
      extras.putLong(EXTRA_MEDIA_ID, playlistItem.mediaItem.getTransientId());
      extras.putLong(EXTRA_ALBUM_ID, playlistItem.mediaItem.getAlbumId());
      extras.putLong(EXTRA_DURATION, playlistItem.mediaItem.getDuration());
      MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
          .setTitle(playlistItem.mediaItem.title)
          .setSubtitle(playlistItem.mediaItem.artist)
          .setIconUri(playlistItem.mediaItem.thumbnailUri)
          .setMediaUri(playlistItem.mediaItem.toUri())
          .setMediaId(playlistItem.mediaItem.getTransientId() + "")
          .setExtras(extras).build();
      MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(description,
          playlistItem.id);
      queue.add(queueItem);
    }
    return queue;
  }

  @Override
  public void onSeekComplete(Player player) {
    updatePlaybackState();
  }


  @Override
  public void onPlayStateChanged(Player player) {
    updatePlaybackState();
    updateMetadata();
  }

  @Override
  public void onQueueChanged() {
    mediaSession.setQueue(buildQueue());
  }

  @Override
  public void onIndexChanged(int oldIndex, int newIndex) {
    updatePlaybackState();
    updateMetadata();
  }
}
