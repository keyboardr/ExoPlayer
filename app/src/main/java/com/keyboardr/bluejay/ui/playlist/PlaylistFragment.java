package com.keyboardr.bluejay.ui.playlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

public class PlaylistFragment extends Fragment {

  private ItemTouchHelper itemTouchHelper;

  public interface Holder {

    @NonNull
    List<MediaSessionCompat.QueueItem> getPlaylist();

    int getCurrentTrackIndex();

    void removeTrack(int index);

    void moveTrack(int oldIndex, int newIndex);

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
    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.playlist_recycler);
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager
        .getOrientation()));

    ItemTouchHelper.SimpleCallback touchCallback = new ItemTouchHelper.SimpleCallback
        (ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      @Override
      public boolean isLongPressDragEnabled() {
        return false;
      }

      @Override
      public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getAdapterPosition() <= getCurrentTrackIndex()) {
          return 0;
        }
        return super.getDragDirs(recyclerView, viewHolder);
      }

      @Override
      public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getAdapterPosition() <= getCurrentTrackIndex()) {
          return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
      }

      @Override
      public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current,
                                 RecyclerView.ViewHolder target) {
        int currentTrackIndex = getCurrentTrackIndex();
        return target.getAdapterPosition() > currentTrackIndex
            && current.getAdapterPosition() > currentTrackIndex
            && super.canDropOver(recyclerView, current, target);
      }


      @Override
      public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
        int oldIndex = viewHolder.getAdapterPosition();
        int newIndex = target.getAdapterPosition();
        getParent().moveTrack(oldIndex, newIndex);
        playlistAdapter.notifyItemMoved(oldIndex, newIndex);
        return false;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int removeIndex = viewHolder.getAdapterPosition();
        getParent().removeTrack(removeIndex);
        playlistAdapter.notifyItemRemoved(removeIndex);
      }
    };
    itemTouchHelper = new ItemTouchHelper(touchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);

    recyclerView.setAdapter(playlistAdapter);
    switcher.setDisplayedChild(playlistAdapter.getItemCount() == 0 ? 0 : 1);
  }

  private Holder getParent() {
    //noinspection ConstantConditions
    return FragmentUtils.getParent(this, Holder.class);
  }

  public void onQueueChanged() {
    playlistAdapter.notifyDataSetChanged();
    switcher.setDisplayedChild(playlistAdapter.getItemCount() == 0 ? 0 : 1);
  }

  public void onIndexChanged(int oldIndex, int newIndex) {
    if (oldIndex >= 0) {
      playlistAdapter.notifyItemChanged(oldIndex);
    }
    playlistAdapter.notifyItemChanged(newIndex);
  }

  private class PlaylistAdapter extends RecyclerView.Adapter<MediaViewHolder> {

    public PlaylistAdapter() {
      setHasStableIds(true);
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new MediaViewHolder(parent, new MediaViewHolder.DragStartListener() {
        @Override
        public void startDrag(@NonNull MediaViewHolder viewHolder) {
          itemTouchHelper.startDrag(viewHolder);
        }

        @Override
        public boolean canDrag(@NonNull MediaItem mediaItem, boolean selected, boolean enabled) {
          return !selected && enabled;
        }
      });
    }

    @Override
    public void onBindViewHolder(MediaViewHolder holder, int position) {
      holder.bindMediaItem(
          PlaylistServiceClient.mediaItemFromQueueItem(getParent().getPlaylist().get(position)),
          position == getCurrentTrackIndex(),
          position >= getCurrentTrackIndex());
    }

    @Override
    public int getItemCount() {
      return getParent().getPlaylist().size();
    }

    @Override
    public long getItemId(int position) {
      return getParent().getPlaylist().get(position).getQueueId();
    }
  }

  private int getCurrentTrackIndex() {
    return getParent().getCurrentTrackIndex();
  }
}
