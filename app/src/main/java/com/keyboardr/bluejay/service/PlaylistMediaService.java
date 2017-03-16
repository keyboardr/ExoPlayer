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
import android.os.PowerManager;
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
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.player.PlaylistPlayer;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlaylistService using MediaBrowserService
 */

public class PlaylistMediaService extends MediaBrowserServiceCompat
    implements Player.PlaybackListener {

  public static final String ACTION_CHECK_IS_ALIVE = "checkIsAlive";

  static final String COMMAND_SET_OUTPUT = "setOutput";
  static final String COMMAND_ADD_TO_QUEUE = "addToQueue";
  static final String COMMAND_MOVE = "move";
  static final String EXTRA_MEDIA_ITEM = "PlaylistMediaService.mediaItem";
  static final String EXTRA_OUTPUT_TYPE = "outputId";
  static final String EXTRA_INDEX = "index";
  static final String EXTRA_NEW_INDEX = "newIndex";
  static final String EXTRA_CONTINUE_ON_DONE = "continueOnDone";
  static final String EXTRA_MEDIA_ID = "mediaId";
  static final String EXTRA_ALBUM_ID = "albumId";
  static final String EXTRA_DURATION = "duration";
  static final String EXTRA_FINISH_TIME = "finishTime";

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
          int outputType = extras.getInt(EXTRA_OUTPUT_TYPE);
          if (outputType == AudioDeviceInfo.TYPE_UNKNOWN) {
            player.setAudioOutput(null);
            updateOutputType();
            return;
          }
          AudioDeviceInfo[] devices = getSystemService(AudioManager.class).getDevices(AudioManager
              .GET_DEVICES_OUTPUTS);
          for (AudioDeviceInfo deviceInfo : devices) {
            if (deviceInfo.getType() == outputType) {
              player.setAudioOutput(deviceInfo);
              updateOutputType();
              return;
            }
          }
          Log.w(TAG, "setPlayerOutput() output not found: " + outputType);
          break;
        case COMMAND_ADD_TO_QUEUE:
          MediaItem mediaItem = extras.getParcelable(EXTRA_MEDIA_ITEM);
          if (mediaItem != null) {
            PlaylistPlayer.PlaylistItem playlistItem = player.addToQueue(mediaItem);
            if (queue == null) {
              queue = new ArrayList<>();
            }
            queue.add(getQueueItem(playlistItem));
            mediaSession.setQueue(queue);
          }
          break;
        case COMMAND_MOVE:
          int oldIndex = extras.getInt(EXTRA_INDEX);
          int newIndex = extras.getInt(EXTRA_NEW_INDEX, -1);

          if (newIndex == -1) {
            player.removeItem(oldIndex);
            queue.remove(oldIndex);
          } else {
            player.moveItem(oldIndex, newIndex);
            queue.add(newIndex, queue.remove(oldIndex));
          }
          mediaSession.setQueue(queue);
      }
    }

  };

  private MediaSessionCompat mediaSession;
  private PlaylistPlayer player;
  private PlaybackStateCompat lastPlaybackState;
  private BroadcastReceiver isAliveReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      setResultCode(Activity.RESULT_OK);
    }
  };

  private PowerManager.WakeLock wakeLock;
  private List<MediaSessionCompat.QueueItem> queue;
  private long finishTime = -1;

  @Override
  public void onCreate() {
    super.onCreate();

    wakeLock = getSystemService(PowerManager.class)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    wakeLock.acquire();
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
    Buses.PLAYLIST.register(this);

    setSessionToken(mediaSession.getSessionToken());

    updateOutputType();
    updateMetadata();
    updatePlaybackState();

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

    Buses.PLAYLIST.unregister(this);

    player.release();
    player = null;
    unregisterReceiver(isAliveReceiver);
    wakeLock.release();
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

  private void updateOutputType() {
    Bundle extras = mediaSession.getController().getExtras();
    if (extras == null) {
      extras = new Bundle();
    }
    extras.putInt(EXTRA_OUTPUT_TYPE, player.getAudioOutputType());
    mediaSession.setExtras(extras);
  }

  private void updatePlaybackState() {
    @PlaybackStateCompat.State int playerState;
    @PlaybackStateCompat.Actions long actions;
    if (player.isStopped()) {
      playerState = PlaybackStateCompat.STATE_STOPPED;
      if (player.getCurrentMediaIndex() < player.getMediaList().size()) {
        actions = PlaybackStateCompat.ACTION_PLAY;
        finishTime = -1;
      } else {
        actions = 0;
        if (finishTime < 0) {
          finishTime = System.currentTimeMillis();
        }
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

    if (!player.isStopped()) {
      finishTime = -1;
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
    extras.putLong(EXTRA_FINISH_TIME, finishTime);
    stateBuilder.setExtras(extras);
    PlaybackStateCompat playbackState = stateBuilder.build();
    if (!isNewPlaybackState(playbackState)) {
      return;
    }
    lastPlaybackState = playbackState;
    mediaSession.setPlaybackState(playbackState);
  }

  private boolean isNewPlaybackState(PlaybackStateCompat playbackState) {
    if (lastPlaybackState == null) {
      return true;
    }
    if (lastPlaybackState.getState() != playbackState.getState()) {
      return true;
    }
    if (lastPlaybackState.getBufferedPosition() != playbackState.getBufferedPosition()) {
      return true;
    }
    if (lastPlaybackState.getActions() != playbackState.getActions()) {
      return true;
    }
    if (lastPlaybackState.getActiveQueueItemId() != playbackState.getActiveQueueItemId()) {
      return true;
    }
    if (lastPlaybackState.getExtras().getBoolean(EXTRA_CONTINUE_ON_DONE)
        != playbackState.getExtras().getBoolean(EXTRA_CONTINUE_ON_DONE)) {
      return true;
    }
    if (lastPlaybackState.getExtras().getInt(EXTRA_INDEX)
        != playbackState.getExtras().getInt(EXTRA_INDEX)) {
      return true;
    }

    return !playbackState.equals(lastPlaybackState);
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
      MediaSessionCompat.QueueItem queueItem = getQueueItem(playlistItem);
      queue.add(queueItem);
    }
    this.queue = queue;
    return queue;
  }

  @NonNull
  private static MediaSessionCompat.QueueItem getQueueItem(PlaylistPlayer.PlaylistItem
                                                               playlistItem) {
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
    return new MediaSessionCompat.QueueItem(description,
        playlistItem.id);
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

  private void onQueueChanged() {
    mediaSession.setQueue(buildQueue());
  }

  @Subscribe
  public void onTrackIndexEvent(@NonNull TrackIndexEvent event) {
    updatePlaybackState();
    updateMetadata();
  }
}
