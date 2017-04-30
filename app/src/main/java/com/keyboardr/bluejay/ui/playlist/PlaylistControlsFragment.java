package com.keyboardr.bluejay.ui.playlist;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.PlaylistErrorEvent;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.AudioSelectionManager;
import com.keyboardr.bluejay.ui.BottomNavHolder;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.Collections;
import java.util.List;

public class PlaylistControlsFragment extends Fragment implements AudioSelectionManager.Callback,
    PlayerControlsUpdater.OnAlbumArtListener {

  @Override
  public boolean canBeDefault(AudioDeviceInfo deviceInfo) {
    return deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE;
  }

  @Override
  public void onNoDeviceFound() {
    PlaylistErrorEvent.addError(Buses.PLAYLIST, PlaylistErrorEvent.ErrorCode.NO_USB_OUTPUT);
  }

  @Override
  public void onDeviceSelected(AudioDeviceInfo audioDeviceInfo) {
    PlaylistErrorEvent.removeError(Buses.PLAYLIST, PlaylistErrorEvent.ErrorCode.NO_USB_OUTPUT);
    player.setAudioOutput(audioDeviceInfo);
  }

  @Override
  public void onAlbumArtReset() {
    BottomNavHolder parent = FragmentUtils.getParent(this, BottomNavHolder.class);
    if (parent != null) {
      parent.setPlaylistAlbumArt(null);
    }
  }

  @Override
  public void onAlbumArtReady(@Nullable Icon albumArt) {
    BottomNavHolder parent = FragmentUtils.getParent(this, BottomNavHolder.class);
    if (parent != null) {
      parent.setPlaylistAlbumArt(albumArt);
    }
  }

  public interface Holder {
    MediaControllerCompat getMediaController();
  }

  private PlaylistServiceClient player;
  private PlayerControlsUpdater uiUpdater;
  private AudioSelectionManager audioSelectionManager;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    player = new PlaylistServiceClient(getParent().getMediaController());
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_playlist_controls, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    uiUpdater = new PlaylistControlsUpdater(view, player, getLoaderManager(), this);
    audioSelectionManager = new AudioSelectionManager(getContext(), this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    uiUpdater.detach();
    uiUpdater = null;
    audioSelectionManager.detach();
    audioSelectionManager = null;
    PlaylistErrorEvent.removeError(Buses.PLAYLIST, PlaylistErrorEvent.ErrorCode.NO_USB_OUTPUT);
  }

  public void addToQueue(@NonNull MediaItem mediaItem) {
    player.addToQueue(mediaItem);
  }

  public void removeItem(int removeIndex) {
    player.removeItem(removeIndex);
  }

  public void moveItem(int oldIndex, int newIndex) {
    player.moveItem(oldIndex, newIndex);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    player.release();
  }

  @NonNull
  public List<MediaSessionCompat.QueueItem> getPlaylist() {
    return player != null ? player.getQueue()
        : Collections.<MediaSessionCompat.QueueItem>emptyList();
  }

  public long getCurrentPosition() {
    return player.getCurrentPosition();
  }

  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }

}
