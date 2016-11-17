package com.keyboardr.bluejay.ui.playlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.player.PlaylistPlayer;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

public class PlaylistFragment extends Fragment {

  private RecyclerView recyclerView;

  public interface Holder {

    List<PlaylistPlayer.PlaylistItem> getPlaylist();

    int getCurrentTrackIndex();

  }

  private PlaylistAdapter playlistAdapter;

  private ViewAnimator switcher;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
      savedInstanceState) {
    return inflater.inflate(R.layout.fragment_playlist, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    switcher = ((ViewAnimator) view.findViewById(R.id.playlist_switcher));
    playlistAdapter = new PlaylistAdapter();
    recyclerView = (RecyclerView) view.findViewById(R.id.playlist_recycler);
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager
        .getOrientation()));
  }

  private Holder getParent() {
    //noinspection ConstantConditions
    return FragmentUtils.getParent(this, Holder.class);
  }

  public void onTrackAdded(int index) {
    playlistAdapter.notifyItemInserted(index);
    switcher.setDisplayedChild(playlistAdapter.getItemCount() == 0 ? 0 : 1);
  }

  public void onIndexChanged(int oldIndex, int newIndex) {
    playlistAdapter.notifyItemChanged(oldIndex);
    playlistAdapter.notifyItemChanged(newIndex);
  }

  public void onMediaListLoaded() {
    recyclerView.setAdapter(playlistAdapter);
    switcher.setDisplayedChild(playlistAdapter.getItemCount() == 0 ? 0 : 1);
  }

  private class PlaylistAdapter extends RecyclerView.Adapter<MediaViewHolder> {

    public PlaylistAdapter() {
      setHasStableIds(true);
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new MediaViewHolder(parent, null);
    }

    @Override
    public void onBindViewHolder(MediaViewHolder holder, int position) {
      holder.bindMediaItem(getParent().getPlaylist().get(position).mediaItem,
          position == getParent().getCurrentTrackIndex(),
          position >= getParent().getCurrentTrackIndex());
    }

    @Override
    public int getItemCount() {
      return getParent().getPlaylist().size();
    }

    @Override
    public long getItemId(int position) {
      return getParent().getPlaylist().get(position).id;
    }
  }
}
