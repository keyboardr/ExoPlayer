package com.keyboardr.bluejay.ui.playlist;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.QueueChangeEvent;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.MonitorContainer;
import com.keyboardr.bluejay.ui.recycler.MediaItemAnimator;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.keyboardr.bluejay.bus.Buses.PlaylistUtils.getCurrentTrackIndex;

public class PlaylistFragment extends Fragment {

  private static final long AUTO_SCROLL_INTERVAL = TimeUnit.SECONDS.toMillis(15);
  private ItemTouchHelper itemTouchHelper;

  private RecyclerView.OnScrollListener interactionListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      lastScroll = SystemClock.elapsedRealtime();
    }
  };
  private RecyclerView recyclerView;

  public interface Holder {

    @NonNull
    List<MediaSessionCompat.QueueItem> getPlaylist();

    void removeTrack(int index);

    void moveTrack(int oldIndex, int newIndex);

  }

  private PlaylistAdapter playlistAdapter;

  private ViewAnimator switcher;

  private long lastScroll;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
      savedInstanceState) {
    return inflater.inflate(R.layout.fragment_playlist, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    lastScroll = SystemClock.elapsedRealtime();
    switcher = ((ViewAnimator) view.findViewById(R.id.playlist_switcher));
    playlistAdapter = new PlaylistAdapter();
    recyclerView = (RecyclerView) view.findViewById(R.id.playlist_recycler);
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager
        .getOrientation()));
    recyclerView.setItemAnimator(new MediaItemAnimator());
    recyclerView.addOnScrollListener(interactionListener);

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

      @Override
      public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
      }
    };
    itemTouchHelper = new ItemTouchHelper(touchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);

    recyclerView.setAdapter(playlistAdapter);
    switcher.setDisplayedChild(playlistAdapter.getItemCount() == 0 ? 0 : 1);
    Buses.PLAYLIST.register(this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Buses.PLAYLIST.unregister(this);
  }

  @NonNull
  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onQueueChanged(@NonNull QueueChangeEvent event) {
    playlistAdapter.notifyDataSetChanged();
    int itemCount = playlistAdapter.getItemCount();
    switcher.setDisplayedChild(itemCount == 0 ? 0 : 1);
    if (itemCount > 0) {
      scrollIfInactive(itemCount - 1);
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onTrackIndexEvent(@NonNull TrackIndexEvent event) {
    if (event.oldIndex >= 0) {
      playlistAdapter.notifyItemChanged(event.oldIndex);
    }
    playlistAdapter.notifyItemChanged(event.newIndex);
    scrollIfInactive(event.newIndex);
  }

  private void scrollIfInactive(int newIndex) {
    if (SystemClock.elapsedRealtime() - lastScroll > AUTO_SCROLL_INTERVAL) {
      recyclerView.scrollToPosition(newIndex);
    }
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

        @Override
        public boolean canClick(@NonNull MediaItem mediaItem) {
          return FragmentUtils.getParent(PlaylistFragment.this, MonitorContainer.class) != null;
        }

        @Override
        public void onMediaItemClicked(@NonNull MediaItem mediaItem) {
          FragmentUtils.getParentChecked(PlaylistFragment.this, MonitorContainer.class)
              .playMediaItemOnMonitor(mediaItem);
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
    public void onBindViewHolder(MediaViewHolder holder, int position, List<Object> payloads) {
      if (payloads == null || payloads.size() == 0) {
        onBindViewHolder(holder, position);
      } else {
        holder.bindMediaItemPartial(position == getCurrentTrackIndex(),
            position >= getCurrentTrackIndex());
      }
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

}
