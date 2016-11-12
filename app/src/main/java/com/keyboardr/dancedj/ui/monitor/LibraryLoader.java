package com.keyboardr.dancedj.ui.monitor;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.os.OperationCanceledException;

import com.keyboardr.dancedj.model.FilterInfo;
import com.keyboardr.dancedj.model.MediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class LibraryLoader extends AsyncTaskLoader<List<MediaItem>> {
    final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
    static final Uri mUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    static final String mSelection = MediaStore.Audio.Media.IS_MUSIC + "=1";

    List<MediaItem> mediaItems;
    CancellationSignal mCancellationSignal;
    private Cursor mCursor;
    @Nullable
    private final FilterInfo filterInfo;

    public LibraryLoader(Context context, @Nullable FilterInfo filterInfo) {
        super(context);
        this.filterInfo = filterInfo;
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
        if (filterInfo != null) {
            Collections.sort(result, filterInfo.getSorting());
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
