package com.keyboardr.bluejay.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
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
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.player.PlaylistPlayer;
import com.keyboardr.bluejay.provider.SetlistContract;
import com.keyboardr.bluejay.provider.SetlistItemContract;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PlaylistService using MediaBrowserService
 */

public class PlaylistMediaService extends MediaBrowserServiceCompat
    implements Player.PlaybackListener {

  public static final String ACTION_CHECK_IS_ALIVE = "com.keyboardr.bluejay.service"
      + ".PlaylistMediaService.checkIsAlive";

  public static final String COMMAND_SET_METADATA = "setMetadata";
  static final String COMMAND_SET_OUTPUT = "setOutput";
  static final String COMMAND_ADD_TO_QUEUE = "addToQueue";
  static final String COMMAND_MOVE = "move";
  static final String COMMAND_SET_VOLUME = "setVolume";

  public static final String EXTRA_SET_METADATA = "setMetadata";
  static final String EXTRA_MEDIA_ITEM = "PlaylistMediaService.mediaItem";
  static final String EXTRA_OUTPUT_TYPE = "outputId";
  static final String EXTRA_INDEX = "index";
  static final String EXTRA_NEW_INDEX = "newIndex";
  static final String EXTRA_NEW_VOLUME = "newVolume";
  static final String PLAYSTATE_MEDIA_ITEM = "mediaItem";
  static final String PLAYSTATE_INDEX = "index";
  static final String PLAYSTATE_CONTINUE_ON_DONE = "continueOnDone";
  static final String PLAYSTATE_FINISH_TIME = "finishTime";
  static final String PLAYSTATE_VOLUME = "volume";
  static final String QUEUE_ALBUM_ID = "albumId";
  static final String QUEUE_DURATION = "duration";
  static final String QUEUE_MEDIA_ID = "mediaId";

  private static final String TAG = "PlaylistMediaService";
  private static final int MIN_RECORDED_SETLIST_SIZE = 3;

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
        case COMMAND_SET_METADATA:
          Bundle metadataBundle = extras.getParcelable(EXTRA_SET_METADATA);
          if (metadataBundle == null) {
            throw new IllegalArgumentException("No SetMetadata in extras");
          }
          SetMetadata setMetadata = new SetMetadata(metadataBundle);
          updateSetMetadata(setMetadata);
          break;
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
          break;
        case COMMAND_SET_VOLUME:
          player.setVolume(extras.getFloat(EXTRA_NEW_VOLUME, 1));
          break;
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

  private SetlistQueryHandler setlistQueryHandler;

  private SetMetadata setMetadata;
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

    setlistQueryHandler = new SetlistQueryHandler(this);

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

    if (player.getMediaList().size() < MIN_RECORDED_SETLIST_SIZE) {
      deleteSetlist();
    }
    setlistQueryHandler.close();

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
    Bundle extras = getSessionExtras();
    extras.putInt(EXTRA_OUTPUT_TYPE, player.getAudioOutputType());
    mediaSession.setExtras(extras);
  }

  @NonNull
  private Bundle getSessionExtras() {
    Bundle extras = mediaSession.getController().getExtras();
    if (extras == null) {
      extras = new Bundle();
    }
    extras.setClassLoader(getClassLoader());
    return extras;
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
    extras.putParcelable(PLAYSTATE_MEDIA_ITEM, currentMediaItem);
    extras.putBoolean(PLAYSTATE_CONTINUE_ON_DONE, player.willContinuePlayingOnDone());
    extras.putInt(PLAYSTATE_INDEX, player.getCurrentMediaIndex());
    extras.putLong(PLAYSTATE_FINISH_TIME, finishTime);
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
    if (!Objects.equals(lastPlaybackState.getExtras().getParcelable(PLAYSTATE_MEDIA_ITEM),
        playbackState.getExtras().getParcelable(PLAYSTATE_MEDIA_ITEM))) {
      return true;
    }
    if (lastPlaybackState.getExtras().getBoolean(PLAYSTATE_CONTINUE_ON_DONE)
        != playbackState.getExtras().getBoolean(PLAYSTATE_CONTINUE_ON_DONE)) {
      return true;
    }
    if (lastPlaybackState.getExtras().getInt(PLAYSTATE_INDEX)
        != playbackState.getExtras().getInt(PLAYSTATE_INDEX)) {
      return true;
    }
    if (lastPlaybackState.getExtras().getLong(PLAYSTATE_FINISH_TIME)
        != playbackState.getExtras().getLong(PLAYSTATE_FINISH_TIME)) {
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
    extras.putLong(QUEUE_MEDIA_ID, playlistItem.mediaItem.getTransientId());
    extras.putLong(QUEUE_ALBUM_ID, playlistItem.mediaItem.getAlbumId());
    extras.putLong(QUEUE_DURATION, playlistItem.mediaItem.getDuration());
    MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
        .setTitle(playlistItem.mediaItem.title)
        .setSubtitle(playlistItem.mediaItem.artist)
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

  @Override
  public void onVolumeChanged(Player player) {
    updatePlaybackState();
  }

  private void onQueueChanged() {
    mediaSession.setQueue(buildQueue());
  }

  @Subscribe
  public void onTrackIndexEvent(@NonNull TrackIndexEvent event) {
    if (hasSetlistId() && event.mediaItem != null) {
      recordItem(event.newIndex, event.mediaItem);
    }
    updatePlaybackState();
    updateMetadata();
  }

  private void updateSetMetadata(@NonNull SetMetadata setMetadata) {
    if (hasSetlistId()) {
      //noinspection ConstantConditions
      setMetadata = SetMetadata.withSetlistId(setMetadata, this.setMetadata.setlistId);
    }
    mediaSession.setQueueTitle(setMetadata.name);
    Bundle sessionExtras = getSessionExtras();
    sessionExtras.putParcelable(EXTRA_SET_METADATA, setMetadata.toBundle());
    mediaSession.setExtras(sessionExtras);
    this.setMetadata = setMetadata;

    if (!setMetadata.isSoundCheck) {
      ContentValues metadataValues = new ContentValues();
      metadataValues.put(SetlistContract.NAME, setMetadata.name);
      if (setMetadata.setlistId == null) {
        setlistQueryHandler.startInsert(SetlistQueryHandler.TOKEN_SETLIST, null,
            SetlistContract.CONTENT_URI,
            metadataValues);
      } else {
        setlistQueryHandler.startUpdate(SetlistQueryHandler.TOKEN_SETLIST, null,
            ContentUris.withAppendedId(SetlistContract.CONTENT_URI, setMetadata.setlistId),
            metadataValues, null, null);
      }
    }
  }

  private void deleteSetlist() {
    Log.d(TAG, "deleteSetlist() called");
    if (hasSetlistId()) {
      //noinspection ConstantConditions
      long setlistId = setMetadata.setlistId;
      setlistQueryHandler.startDelete(SetlistQueryHandler.TOKEN_SETLIST, null,
          ContentUris.withAppendedId(SetlistContract.CONTENT_URI, setlistId),
          null, null);
      setlistQueryHandler.startDelete(SetlistQueryHandler.TOKEN_ITEM, null,
          SetlistItemContract.CONTENT_URI,
          SetlistItemContract.SETLIST_ID + " = ?",
          new String[]{Long.toString(setlistId)});
    }
  }

  private boolean hasSetlistId() {
    return setMetadata != null && setMetadata.setlistId != null;
  }

  private void recordCurrentPlayedQueue() {
    if (!hasSetlistId()) {
      throw new IllegalStateException("Cant record queue with no setlistId");
    }

    List<PlaylistPlayer.PlaylistItem> mediaItems = player.getMediaList();
    int currentIndex = player.getCurrentMediaIndex();
    for (int i = 0; i < mediaItems.size() && i <= currentIndex; i++) {
      MediaItem mediaItem = mediaItems.get(i).mediaItem;
      recordItem(i, mediaItem);
    }
  }

  private void recordItem(int position, MediaItem mediaItem) {
    Log.d(TAG,
        "recordItem() called with: position = [" + position + "], mediaItem = [" + mediaItem + "]");
    ContentValues values = new ContentValues();
    values.put(SetlistItemContract.POSITION, position);
    values.put(SetlistItemContract.ARTIST, mediaItem.artist.toString());
    values.put(SetlistItemContract.TITLE, mediaItem.title.toString());
    values.put(SetlistItemContract.MEDIA_ID, mediaItem.getTransientId());
    values.put(SetlistItemContract.SETLIST_ID, setMetadata.setlistId);
    setlistQueryHandler.startInsert(SetlistQueryHandler.TOKEN_ITEM, null, SetlistItemContract
        .CONTENT_URI, values);
  }

  private static class SetlistQueryHandler extends AsyncQueryHandler {
    public static final int TOKEN_SETLIST = 0;
    public static final int TOKEN_ITEM = 1;

    @Nullable
    private PlaylistMediaService service;

    public SetlistQueryHandler(@NonNull PlaylistMediaService service) {
      super(service.getContentResolver());
      this.service = service;
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
      if (token == TOKEN_SETLIST) {
        if (service != null) {
          service.updateSetMetadata(SetMetadata.withSetlistId(service.setMetadata,
              ContentUris.parseId(uri)));
          service.recordCurrentPlayedQueue();
        }
      }
    }

    public void close() {
      this.service = null;
    }
  }
}
