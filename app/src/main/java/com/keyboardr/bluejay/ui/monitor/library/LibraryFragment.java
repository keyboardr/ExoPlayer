package com.keyboardr.bluejay.ui.monitor.library;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.ui.monitor.MediaAdapter;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.List;

public class LibraryFragment extends android.support.v4.app.Fragment implements MediaViewHolder.OnMediaItemSelectedListener,
        MediaViewHolder.MediaViewDecorator, FilterFragment.Holder {

    private static final int SWITCHER_LOADING = 0;
    private static final int SWITCHER_EMPTY = 1;
    private static final int SWITCHER_LOADED = 2;
    private static final String ARG_FILTER = "filter";
    private ViewAnimator switcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        switcher = (ViewAnimator) view.findViewById(R.id.library_switcher);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.library_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
        new ItemTouchHelper(getItemTouchHelperCallback()).attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(getAdapter());
        return view;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected ItemTouchHelper.Callback getItemTouchHelperCallback() {
        return new ItemTouchHelper.SimpleCallback(0, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(0, null, mediaLoaderCallbacks);
    }

    @Override
    public void setLibraryFilter(FilterInfo filter) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILTER, filter);
        getLoaderManager().restartLoader(0, args, mediaLoaderCallbacks);
    }

    public interface LibraryFragmentHolder {
        void playMediaItemOnMonitor(@NonNull MediaItem mediaItem);

        boolean canAddToQueue();

        void addToQueue(@NonNull MediaItem mediaItem);
    }

    private final LoaderManager.LoaderCallbacks<List<MediaItem>> mediaLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<MediaItem>>() {
        @Override
        public Loader<List<MediaItem>> onCreateLoader(int i, Bundle bundle) {
            return new LibraryLoader(getContext(), bundle == null ? null :((FilterInfo) bundle.getParcelable(ARG_FILTER)));
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

    private final MediaAdapter adapter = new MediaAdapter(this, this);

    @NonNull
    protected RecyclerView.Adapter getAdapter() {
        return adapter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentUtils.checkParent(this, LibraryFragmentHolder.class);
    }

    @NonNull
    protected LibraryFragmentHolder getParent() {
        // Checked in #onAttach(Context)
        //noinspection ConstantConditions
        return FragmentUtils.getParent(this, LibraryFragmentHolder.class);
    }

    public void notifyConnectionChanged() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaItemSelected(MediaItem mediaItem) {
        getParent().playMediaItemOnMonitor(mediaItem);
    }

    @Override
    @DrawableRes
    public int getIconForItem(MediaItem mediaItem) {
        return getParent().canAddToQueue() ? R.drawable.ic_playlist_add : 0;
    }

    @Override
    public void onDecoratorSelected(MediaItem mediaItem) {
        getParent().addToQueue(mediaItem);
    }

}
