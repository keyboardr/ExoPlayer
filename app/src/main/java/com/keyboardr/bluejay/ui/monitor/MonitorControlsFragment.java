package com.keyboardr.bluejay.ui.monitor;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.MonitorPlayer;
import com.keyboardr.bluejay.ui.AudioSelectionManager;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.CachedLoader;

public class MonitorControlsFragment extends Fragment implements AudioSelectionManager.Callback {

  @SuppressWarnings("unused")
  public static MonitorControlsFragment newInstance() {
    return new MonitorControlsFragment();
  }

  private PlayerControlsUpdater uiUpdater;
  private MonitorPlayer player;
  private AudioSelectionManager audioSelectionManager;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    player = new MonitorPlayer(getContext());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_monitor_controls, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    audioSelectionManager = new AudioSelectionManager(getContext(), this);
    uiUpdater = new MonitorControlsUpdater(view, player, getLoaderManager());
  }

  @Override
  public void onDestroyView() {
    uiUpdater.detach();
    audioSelectionManager.detach();
    super.onDestroyView();
  }

  public void playMedia(MediaItem mediaItem) {
    player.play(mediaItem, true);
    uiUpdater.onMetaData();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    player.release();
  }

  @Nullable
  public MediaItem getCurrentTrack() {
    return player.getCurrentMediaItem();
  }

  @Override
  public boolean canBeDefault(AudioDeviceInfo deviceInfo) {
    return deviceInfo.getType() != AudioDeviceInfo.TYPE_USB_DEVICE;
  }

  @Override
  public void onNoDeviceFound() {
    player.setAudioOutput(null);
  }

  @Override
  public void onDeviceSelected(AudioDeviceInfo audioDeviceInfo) {
    player.setAudioOutput(audioDeviceInfo);
  }

  public static class AlbumArtLoader extends CachedLoader<Pair<Icon, Palette>> {

    private final MediaItem mediaItem;

    public AlbumArtLoader(@NonNull Context context, @Nullable MediaItem item) {
      super(context);
      this.mediaItem = item;
    }

    @Override
    @Nullable
    public Pair<Icon, Palette> loadInBackground() {
      if (mediaItem == null) {
        return null;
      }
      return mediaItem.getAlbumArt(getContext(), getContext().getResources()
          .getDimensionPixelSize(R.dimen.controls_album_art_size));
    }
  }
}
