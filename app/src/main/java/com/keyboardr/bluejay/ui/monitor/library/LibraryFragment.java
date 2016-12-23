package com.keyboardr.bluejay.ui.monitor.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

public class LibraryFragment extends android.support.v4.app.Fragment
    implements FilterFragment.Holder, MetadataFragment.Holder {

  public interface Holder {
    void playMediaItemOnMonitor(@NonNull MediaItem mediaItem);

    boolean canAddToQueue();

    void addToQueue(@NonNull MediaItem mediaItem);
  }

  private static final int SWITCHER_LOADING = 0;
  private static final int SWITCHER_EMPTY = 1;
  private static final int SWITCHER_LOADED = 2;
  private static final String ARG_FILTER = "filter";

  private ViewAnimator switcher;

  private final LoaderManager.LoaderCallbacks<List<MediaItem>> mediaLoaderCallbacks
      = new LoaderManager.LoaderCallbacks<List<MediaItem>>() {
    @Override
    public Loader<List<MediaItem>> onCreateLoader(int i, Bundle bundle) {
      return new LibraryLoader(getContext(), bundle == null ? null : ((FilterInfo) bundle
          .getParcelable(ARG_FILTER)), getShortlistManager());
    }

    @Override
    public void onLoadFinished(Loader<List<MediaItem>> loader, List<MediaItem> items) {
      adapter.setMediaItems(items);
      switcher.setDisplayedChild(items.size() != 0
          ? LibraryFragment.SWITCHER_LOADED : LibraryFragment.SWITCHER_EMPTY);
    }

    @Override
    public void onLoaderReset(Loader<List<MediaItem>> loader) {
      switcher.setDisplayedChild(LibraryFragment.SWITCHER_LOADING);
    }
  };

  private final MediaViewHolder.MediaViewDecorator decorator
      = new MediaViewHolder.MediaViewDecorator() {

    @Override
    public void onMediaItemSelected(@NonNull MediaItem mediaItem) {
      getParent().playMediaItemOnMonitor(mediaItem);
    }

    @Override
    @DrawableRes
    public int getIconForItem(@NonNull MediaItem mediaItem) {
      return getParent().canAddToQueue() ? R.drawable.ic_playlist_add : 0;
    }

    @Override
    public void onDecoratorSelected(@NonNull MediaItem mediaItem) {
      getParent().addToQueue(mediaItem);
    }

    @Override
    public boolean showMoreOption() {
      return shortlistManager.isReady();
    }

    @Override
    public void onMoreSelected(@NonNull MediaItem mediaItem) {
      MetadataFragment.show(LibraryFragment.this, mediaItem);
    }

  };

  private final MediaAdapter adapter = new MediaAdapter(decorator);

  private ShortlistManager shortlistManager;

  private BroadcastReceiver shortlistsReadyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (shortlistManager.isReady()) {
        adapter.notifyDataSetChanged();
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(this);
      }
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    shortlistManager = new ShortlistManager(getContext());
    LocalBroadcastManager.getInstance(getContext().getApplicationContext()).registerReceiver
        (shortlistsReadyReceiver, new IntentFilter(ShortlistManager.ACTION_SHORTLISTS_READY));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(getContext().getApplicationContext()).unregisterReceiver
        (shortlistsReadyReceiver);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_library, container, false);
    switcher = (ViewAnimator) view.findViewById(R.id.library_switcher);
    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.library_recycler);
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
        layoutManager.getOrientation()));
    recyclerView.setAdapter(adapter);
    return view;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    getLoaderManager().initLoader(0, null, mediaLoaderCallbacks);
  }

  @Override
  public ShortlistManager getShortlistManager() {
    return shortlistManager;
  }

  @Override
  public void setLibraryFilter(FilterInfo filter) {
    Bundle args = new Bundle();
    args.putParcelable(ARG_FILTER, filter);
    getLoaderManager().restartLoader(0, args, mediaLoaderCallbacks);
  }

  @NonNull
  protected Holder getParent() {
    // Checked in #onAttach(Context)
    //noinspection ConstantConditions
    return FragmentUtils.getParent(this, Holder.class);
  }

  public void notifyConnectionChanged() {
    adapter.notifyDataSetChanged();
  }

}
