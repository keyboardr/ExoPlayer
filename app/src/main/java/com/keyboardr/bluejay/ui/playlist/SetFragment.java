package com.keyboardr.bluejay.ui.playlist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

public class SetFragment extends Fragment implements PlaylistFragment.Holder,
    PlaylistControlsFragment.Holder {

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
    setHasOptionsMenu(true);
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

  private PlaylistControlsFragment getPlaylistControlsFragment() {
    return (PlaylistControlsFragment) getChildFragmentManager().findFragmentById(
        R.id.playlist_control_fragment);
  }

  private PlaylistFragment getPlaylistFragment() {
    return (PlaylistFragment) getChildFragmentManager().findFragmentById(R.id.playlist_fragment);
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
  public int getCurrentTrackIndex() {
    return getPlaylistControlsFragment().getCurrentTrackIndex();
  }

  @Override
  public void onQueueChanged() {
    getPlaylistFragment().onQueueChanged();
  }

  @Override
  public void onIndexChanged(int oldIndex, int newIndex) {
    getPlaylistFragment().onIndexChanged(oldIndex, newIndex);
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

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.frag_set, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.end_set:
        new EndSetDialogFragment().show(getChildFragmentManager(), null);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void endSetConfirmed() {
    mediaController.getTransportControls().stop();
    //noinspection ConstantConditions
    FragmentUtils.getParent(this, Holder.class).endSet();
  }

  public static class EndSetDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return new AlertDialog.Builder(getContext()).setTitle(R.string.end_set)
          .setMessage(R.string.end_set_dialog_message)
          .setPositiveButton(android.R.string.yes,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  //noinspection ConstantConditions
                  FragmentUtils.getParent(EndSetDialogFragment.this, SetFragment.class)
                      .endSetConfirmed();
                  dialogInterface.dismiss();
                }
              }).setNegativeButton(android.R.string.cancel,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  dialogInterface.cancel();
                }
              }).create();
    }

  }
}
