package com.keyboardr.dancedj;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.ui.MediaCursorAdapter;
import com.keyboardr.dancedj.ui.MediaViewHolder;
import com.keyboardr.dancedj.ui.RecyclerFragment;
import com.keyboardr.dancedj.util.FragmentUtils;

public class LibraryFragment extends RecyclerFragment implements MediaViewHolder.OnMediaItemSelectedListener {

    public interface LibraryFragmentHolder {
        void playMediaItemOnMonitor(@NonNull MediaItem mediaItem);
        void addToQueue(@NonNull MediaItem mediaItem);
    }

    public static LibraryFragment newInstance() {
        return new LibraryFragment();
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

    private final MediaCursorAdapter adapter = new MediaCursorAdapter(null, this);

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

    @Override
    public void onMediaItemSelected(MediaItem mediaItem) {
        getParent().addToQueue(mediaItem);
    }
}
