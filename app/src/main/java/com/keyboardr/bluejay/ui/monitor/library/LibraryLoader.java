package com.keyboardr.bluejay.ui.monitor.library;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.PermissionChecker;
import android.support.v4.os.OperationCanceledException;

import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.provider.SetlistItemContract;
import com.keyboardr.bluejay.provider.ShortlistManager;

import java.util.ArrayList;
import java.util.List;

class LibraryLoader extends AsyncTaskLoader<List<MediaItem>> {
  final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
  static final Uri mLibraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
  static final String mLibrarySelection = MediaStore.Audio.Media.IS_MUSIC + "=1";

  @NonNull
  private final ShortlistManager shortlistManager;

  List<MediaItem> mediaItems;
  CancellationSignal mCancellationSignal;
  private Cursor mCursor;
  @NonNull
  private final FilterInfo filterInfo;

  private volatile boolean canceled;

  public LibraryLoader(Context context, @NonNull FilterInfo filterInfo, @NonNull
      ShortlistManager shortlistManager) {
    super(context);
    this.filterInfo = filterInfo;
    this.shortlistManager = shortlistManager;
  }

  @Nullable
  @Override
  public List<MediaItem> loadInBackground() {
    synchronized (this) {
      if (isLoadInBackgroundCanceled()) {
        throw new OperationCanceledException();
      }
      mCancellationSignal = new CancellationSignal();
    }
    canceled = false;

    if (PermissionChecker.checkSelfPermission(getContext(),
        Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
      return null;
    }

    try {
      if (filterInfo.setlistId == null) {
        String sortOrder = filterInfo.getSortColumn();
        Cursor cursor = getContext().getContentResolver().query(mLibraryUri, null,
            mLibrarySelection, null, sortOrder, mCancellationSignal);
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
        return processLibraryCursor(cursor);
      } else {
        try (Cursor cursor = getContext().getContentResolver().query(SetlistItemContract
            .CONTENT_URI, null, SetlistItemContract.SETLIST_ID + "=?", new String[]{Long.toString
            (filterInfo.setlistId)}, SetlistItemContract.POSITION, mCancellationSignal)) {
          return processSetlistCursor(cursor);
        }
      }
    } finally {
      synchronized (this) {
        mCancellationSignal = null;
      }
    }
  }

  @Nullable
  @WorkerThread
  private List<MediaItem> processLibraryCursor(@Nullable Cursor cursor) {
    if (mCursor != null && mCursor != cursor) {
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
          MediaItem item = MediaItem.build()
              .setArtist(cursor.getString(artistColumn))
              .setTitle(cursor.getString(titleColumn))
              .setAlbumId(cursor.getLong(albumIdColumn))
              .setDuration(cursor.getLong(durationColumn))
              .setPath(cursor.getString(dataColumn))
              .make(cursor.getLong(mediaIdColumn));
          if (filterInfo.isAllowed(item, shortlistManager)) {
            result.add(item);
          }
        } while (cursor.moveToNext() && !canceled);
      }
    }
    return canceled ? null : result;
  }

  @Nullable
  @WorkerThread
  private List<MediaItem> processSetlistCursor(@Nullable Cursor cursor) {
    if (mCursor != null && mCursor != cursor) {
      mCursor.close();
    }
    List<MediaItem> result = new ArrayList<>();
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        int artistColumn = cursor.getColumnIndexOrThrow(SetlistItemContract.ARTIST);
        int titleColumn = cursor.getColumnIndexOrThrow(SetlistItemContract.TITLE);
        int mediaIdColumn = cursor.getColumnIndexOrThrow(SetlistItemContract.MEDIA_ID);
        do {
          long mediaId = cursor.getLong(mediaIdColumn);
          MediaItem mediaItem = getMediaItemFromId(mediaId);
          if (mediaItem == null) {
            mediaItem = MediaItem.build().setArtist(cursor.getString(artistColumn)).setTitle(cursor
                .getString(titleColumn)).make(mediaId);
          }
          result.add(mediaItem);
        } while (cursor.moveToNext() && !canceled);
      }
    }
    return canceled ? null : result;
  }

  @WorkerThread
  @Nullable
  private MediaItem getMediaItemFromId(long id) {
    try (Cursor cursor = getContext().getContentResolver().query(
        ContentUris.withAppendedId(mLibraryUri, id), null, mLibrarySelection, null, null,
        mCancellationSignal)) {
      if (cursor != null && cursor.moveToFirst()) {
        int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
        int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        int mediaIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        return MediaItem.build()
            .setArtist(cursor.getString(artistColumn))
            .setTitle(cursor.getString(titleColumn))
            .setAlbumId(cursor.getLong(albumIdColumn))
            .setDuration(cursor.getLong(durationColumn))
            .setPath(cursor.getString(dataColumn))
            .make(cursor.getLong(mediaIdColumn));
      }
    }
    return null;
  }


  @Override
  public void cancelLoadInBackground() {
    super.cancelLoadInBackground();
    synchronized (this) {
      if (mCancellationSignal != null) {
        mCancellationSignal.cancel();
        canceled = true;
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
