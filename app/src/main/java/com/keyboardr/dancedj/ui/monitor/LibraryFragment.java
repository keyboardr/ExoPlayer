package com.keyboardr.dancedj.ui.monitor;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.ui.recycler.MediaCursorAdapter;
import com.keyboardr.dancedj.ui.recycler.MediaViewHolder;
import com.keyboardr.dancedj.ui.recycler.RecyclerFragment;
import com.keyboardr.dancedj.util.FragmentUtils;

public class LibraryFragment extends RecyclerFragment implements MediaViewHolder.OnMediaItemSelectedListener, MediaViewHolder.MediaViewDecorator {

    public interface LibraryFragmentHolder {
        void playMediaItemOnMonitor(@NonNull MediaItem mediaItem);
        boolean canAddToQueue();
        void addToQueue(@NonNull MediaItem mediaItem);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mediaLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            adapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private final MediaCursorAdapter adapter = new MediaCursorAdapter(null, this, this);

    @NonNull
    @Override
    protected LoaderManager.LoaderCallbacks getLoaderCallbacks() {
        return mediaLoaderCallbacks;
    }

    @NonNull
    @Override
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
