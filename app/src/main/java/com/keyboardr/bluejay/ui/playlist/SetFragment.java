package com.keyboardr.bluejay.ui.playlist;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

import static com.keyboardr.bluejay.bus.Buses.PlaylistUtils.getCurrentTrackIndex;

public class SetFragment extends Fragment implements PlaylistFragment.Holder,
    PlaylistControlsFragment.Holder, SetInfoFragment.Holder {
  public interface Holder {
    void endSet();
  }

  private static final String ARG_SESSION_TOKEN = "sessionToken";
  private MediaControllerCompat mediaController;

  public static SetFragment newInstance(MediaSessionCompat.Token token) {
    Bundle args = new Bundle();
    args.putParcelable(ARG_SESSION_TOKEN, token);
    SetFragment setFragment = new SetFragment();
    setFragment.setArguments(args);
    return setFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      MediaSessionCompat.Token sessionToken = getArguments().getParcelable(ARG_SESSION_TOKEN);
      if (sessionToken == null) {
        throw new IllegalArgumentException("No session token provided");
      }
      mediaController = new MediaControllerCompat(getContext(), sessionToken);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

  public void addToQueue(@NonNull MediaItem mediaItem) {
    getPlaylistControlsFragment().addToQueue(mediaItem);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_setlist, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.set_info_fragment).setTransitionName(
        getString(R.string.shared_element_bottom_bar));
  }

  private PlaylistControlsFragment getPlaylistControlsFragment() {
    return (PlaylistControlsFragment) getChildFragmentManager().findFragmentById(
        R.id.playlist_control_fragment);
  }

  @Override
  public MediaControllerCompat getMediaController() {
    return mediaController;
  }

  @NonNull
  @Override
  public List<MediaSessionCompat.QueueItem> getPlaylist() {
    return getPlaylistControlsFragment().getPlaylist();
  }

  @Override
  public long getCurrentPosition() {
    return getPlaylistControlsFragment().getCurrentPosition();
  }

  @Override
  public void removeTrack(int removeIndex) {
    int size = getPlaylist().size();
    if (removeIndex >= size) {
      throw new IndexOutOfBoundsException("Attempted to remove index " + removeIndex
          + " from playlist size " + size);
    }
    int currentTrackIndex = getCurrentTrackIndex();
    if (removeIndex <= currentTrackIndex) {
      throw new IndexOutOfBoundsException("Attempted to remove index " + removeIndex
          + ", current track index: " + currentTrackIndex);
    }
    getPlaylistControlsFragment().removeItem(removeIndex);
  }

  @Override
  public void moveTrack(int oldIndex, int newIndex) {
    int size = getPlaylist().size();
    if (oldIndex >= size || newIndex >= size) {
      throw new IndexOutOfBoundsException("Attempted to move index " + oldIndex
          + " to " + newIndex + " from playlist size " + size);
    }
    int currentTrackIndex = getCurrentTrackIndex();
    if (oldIndex <= currentTrackIndex || newIndex <= currentTrackIndex) {
      throw new IndexOutOfBoundsException("Attempted to move index " + oldIndex
          + " to " + newIndex + ", current track index: " + currentTrackIndex);
    }
    getPlaylistControlsFragment().moveItem(oldIndex, newIndex);
  }

  public boolean queueContains(MediaItem mediaItem) {
    for (MediaSessionCompat.QueueItem queueItem : getPlaylist()) {
      if (Long.valueOf(queueItem.getDescription().getMediaId()).equals(
          mediaItem.getTransientId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void endSet() {
    mediaController.getTransportControls().stop();
    //noinspection ConstantConditions
    FragmentUtils.getParent(this, Holder.class).endSet();
  }
}
