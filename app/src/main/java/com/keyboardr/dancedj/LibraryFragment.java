package com.keyboardr.dancedj;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.keyboardr.dancedj.ui.MediaCursorAdapter;
import com.keyboardr.dancedj.ui.RecyclerFragment;

public class LibraryFragment extends RecyclerFragment {

    public static LibraryFragment newInstance() {
        return new LibraryFragment();
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mediaLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            adapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private final MediaCursorAdapter adapter = new MediaCursorAdapter(null);

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
}
