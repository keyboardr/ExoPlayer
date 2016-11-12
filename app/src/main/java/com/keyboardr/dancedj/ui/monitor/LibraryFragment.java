package com.keyboardr.dancedj.ui.monitor;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.os.OperationCanceledException;
import android.support.v7.widget.RecyclerView;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.ui.recycler.MediaViewHolder;
import com.keyboardr.dancedj.ui.recycler.RecyclerFragment;
import com.keyboardr.dancedj.util.FragmentUtils;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends RecyclerFragment implements MediaViewHolder.OnMediaItemSelectedListener, MediaViewHolder.MediaViewDecorator {

    public interface LibraryFragmentHolder {
        void playMediaItemOnMonitor(@NonNull MediaItem mediaItem);

        boolean canAddToQueue();

        void addToQueue(@NonNull MediaItem mediaItem);
    }

    private final LoaderManager.LoaderCallbacks<List<MediaItem>> mediaLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<MediaItem>>() {
        @Override
        public Loader<List<MediaItem>> onCreateLoader(int i, Bundle bundle) {
            return new LibraryLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<List<MediaItem>> loader, List<MediaItem> items) {
            adapter.setMediaItems(items);
        }

        @Override
        public void onLoaderReset(Loader<List<MediaItem>> loader) {
        }
    };

    private final MediaAdapter adapter = new MediaAdapter(this, this);

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

    private static class LibraryLoader extends AsyncTaskLoader<List<MediaItem>> {
        final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
        static final Uri mUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        static final String mSelection = MediaStore.Audio.Media.IS_MUSIC + "=1";

        List<MediaItem> mediaItems;
        CancellationSignal mCancellationSignal;
        private Cursor mCursor;

        public LibraryLoader(Context context) {
            super(context);
        }

        @Override
        public List<MediaItem> loadInBackground() {
            synchronized (this) {
                if (isLoadInBackgroundCanceled()) {
                    throw new OperationCanceledException();
                }
                mCancellationSignal = new CancellationSignal();
            }
            try {
                Cursor cursor = getContext().getContentResolver().query(mUri, null, mSelection,
                        null, null, mCancellationSignal);
                if (cursor != null) {
                    try {
                        // Ensure the cursor window is filled.
                        cursor.getCount();
                        cursor.registerContentObserver(mObserver);
                    } catch (RuntimeException ex) {
                        cursor.close();
                        throw ex;
                    }
                }
                return processCursor(cursor);
            } finally {
                synchronized (this) {
                    mCancellationSignal = null;
                }
            }
        }

        private List<MediaItem> processCursor(@Nullable Cursor cursor) {
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = cursor;
            List<MediaItem> result = new ArrayList<>();
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int mediaIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    do {
                        result.add(MediaItem.build()
                                .setArtist(cursor.getString(artistColumn))
                                .setTitle(cursor.getString(titleColumn))
                                .setAlbumId(cursor.getLong(albumIdColumn))
                                .setDuration(cursor.getLong(durationColumn))
                                .setPath(cursor.getString(dataColumn))
                                .make(cursor.getLong(mediaIdColumn)));
                    } while (cursor.moveToNext());
                }
            }
            return result;
        }

        @Override
        public void cancelLoadInBackground() {
            super.cancelLoadInBackground();
            synchronized (this) {
                if (mCancellationSignal != null) {
                    mCancellationSignal.cancel();
                }
            }
        }

        /* Runs on the UI thread */
        @Override
        public void deliverResult(List<MediaItem> mediaItems) {
            if (isReset()) {
                return;
            }
            if (isStarted()) {
                super.deliverResult(mediaItems);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mediaItems != null) {
                deliverResult(mediaItems);
            }
            if (takeContentChanged() || mediaItems == null) {
                forceLoad();
            }
        }

        /**
         * Must be called from the UI thread
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();
            mediaItems = null;
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
        }

    }
}
