package com.keyboardr.bluejay.provider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.model.Shortlist;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShortlistManager {

  public static final String ACTION_SHORTLISTS_READY = "com.keyboardr.bluejay.ui.monitor"
      + ".ShortlistManager.ACTION_SHORTLISTS_READY";

  public static final String ACTION_SHORTLIST_PAIRS_READY = "com.keyboardr.bluejay.ui.monitor"
      + ".ShortlistManager.ACTION_SHORTLIST_PAIRS_READY";

  public static final String ACTION_SHORTLISTS_CHANGED = "com.keyboardr.bluejay.ui.monitor"
      + "ShortlistManager.ACTION_SHORTLISTS_CHANGED";

  private static final String TAG = "ShortlistManager";

  // Indexed by media id, Sorted by shortlist id
  @Nullable
  private LongSparseArray<List<Shortlist>> shortlistMap;

  private List<Pair<MediaItem, Shortlist>> pendingAdds = new ArrayList<>();
  private List<Pair<MediaItem, Shortlist>> pendingRemoves = new ArrayList<>();
  private LongSparseArray<Shortlist> shortlists;

  private final AsyncQueryHandler queryHandler;
  private final LocalBroadcastManager localBroadcastManager;

  private List<Shortlist> cachedShortlists;

  public ShortlistManager(@NonNull Context context) {
    queryHandler = new ShortlistQueryHandler(new WeakReference<>(this),
        context.getContentResolver());
    localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
    queryHandler.startQuery(ShortlistQueryHandler.TOKEN_INIT_SHORTLIST, null,
        ShortlistsContract.CONTENT_URI, null, null, null, ShortlistsContract._ID);
  }

  @Nullable
  public List<Shortlist> getShortlists(@NonNull MediaItem mediaItem) {
    if (shortlistMap == null) {
      return null;
    }
    List<Shortlist> result = shortlistMap.get(mediaItem.getTransientId());
    if (result == null) {
      result = new ArrayList<>();
      shortlistMap.put(mediaItem.getTransientId(), result);
    }

    // Remove deleted shortlists
    for (int i = result.size() - 1; i >= 0; i--) {
      if (shortlists.get(result.get(i).getId()) == null) {
        result.remove(i);
      }
    }
    return result;
  }

  public void add(@NonNull MediaItem mediaItem, @NonNull Shortlist shortlist) {
    // Cancel pending removal of this pair
    for (int i = pendingRemoves.size() - 1; i >= 0; i--) {
      Pair<MediaItem, Shortlist> pendingRemoval = pendingRemoves.get(i);
      if (pendingRemoval.first.equals(mediaItem)
          && pendingRemoval.second.equals(shortlist)) {
        pendingRemoves.remove(i);
      }
    }

    List<Shortlist> shortlists = getShortlists(mediaItem);
    if (shortlists == null) {
      pendingAdds.add(new Pair<>(mediaItem, shortlist));
      return;
    }

    int index = Collections.binarySearch(shortlists, shortlist);
    if (index < 0) {
      shortlists.add(-index - 1, shortlist);
      ContentValues values = new ContentValues();
      values.put(MediaShortlistContract.MEDIA_ID, mediaItem.getTransientId());
      values.put(MediaShortlistContract.SHORTLIST_ID, shortlist.getId());
      queryHandler.startInsert(0, null, MediaShortlistContract.CONTENT_URI, values);
    }
  }

  public void remove(@NonNull MediaItem mediaItem, @NonNull Shortlist shortlist) {
    // Cancel pending addition of this pair
    for (int i = pendingAdds.size() - 1; i >= 0; i--) {
      Pair<MediaItem, Shortlist> pendingAdd = pendingAdds.get(i);
      if (pendingAdd.first.equals(mediaItem)
          && pendingAdd.second.equals(shortlist)) {
        pendingAdds.remove(i);
      }
    }

    List<Shortlist> shortlists = getShortlists(mediaItem);
    if (shortlists == null) {
      pendingRemoves.add(new Pair<>(mediaItem, shortlist));
      return;
    }

    int index = Collections.binarySearch(shortlists, shortlist);
    if (index >= 0) {
      shortlists.add(index, shortlist);
    }
    queryHandler.startDelete(0, null, MediaShortlistContract.CONTENT_URI,
        MediaShortlistContract.SHORTLIST_ID + " = ? AND "
            + MediaShortlistContract.MEDIA_ID + " = ?",
        new String[]{shortlist.getId() + "", mediaItem.getTransientId() + ""});
  }

  public List<Shortlist> getShortlists() {
    if (cachedShortlists != null) {
      return cachedShortlists;
    }
    ArrayList<Shortlist> result = new ArrayList<>(shortlists.size());
    for (int i = 0; i < shortlists.size(); i++) {
      result.add(shortlists.valueAt(i));
    }
    cachedShortlists = Collections.unmodifiableList(result);
    return result;
  }

  public boolean isInShortlist(@NonNull MediaItem mediaItem, @NonNull Shortlist shortlist) {
    List<Shortlist> shortlists = getShortlists(mediaItem);
    return shortlists != null && Collections.binarySearch(shortlists, shortlist) >= 0;
  }

  public void createShortlist(@NonNull String name) {
    if (shortlists == null) {
      throw new IllegalStateException("Shortlists not yet initialized");
    }
    ContentValues values = new ContentValues();
    values.put(ShortlistsContract.NAME, name);
    queryHandler.startInsert(ShortlistQueryHandler.TOKEN_SHORTLIST, null,
        ShortlistsContract.CONTENT_URI, values);
  }

  public void deleteShortlist(@NonNull Shortlist shortlist) {
    if (shortlists == null) {
      throw new IllegalStateException("Shortlists not yet initialized");
    }
    shortlists.remove(shortlist.getId());
    cachedShortlists = null;
    queryHandler.startDelete(0, null, ContentUris.withAppendedId(ShortlistsContract.CONTENT_URI,
        shortlist.getId()), null, null);
    queryHandler.startDelete(0, null, MediaShortlistContract.CONTENT_URI,
        MediaShortlistContract.SHORTLIST_ID + " = " + shortlist.getId(), null);
    notifyShortlistsChanged();
  }

  public boolean isReady() {
    return shortlists != null && shortlistMap != null;
  }

  private void notifyShortlistsChanged() {
    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLISTS_CHANGED));
  }

  private void setShortlists(@NonNull List<Shortlist> shortlists) {
    this.shortlists = new LongSparseArray<>(shortlists.size());
    cachedShortlists = null;
    for (Shortlist shortlist : shortlists) {
      this.shortlists.put(shortlist.getId(), shortlist);
    }
    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLISTS_CHANGED));
    queryHandler.startQuery(ShortlistQueryHandler.TOKEN_INIT_PAIRS, null,
        MediaShortlistContract.CONTENT_URI, null, null, null,
        MediaShortlistContract.MEDIA_ID);
  }

  private void setShortlistPairs(LongSparseArray<List<Shortlist>> shortlistMap) {
    this.shortlistMap = shortlistMap;

    for (Pair<MediaItem, Shortlist> add : pendingAdds) {
      add(add.first, add.second);
    }
    for (Pair<MediaItem, Shortlist> remove : pendingRemoves) {
      remove(remove.first, remove.second);
    }

    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLIST_PAIRS_READY));
    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLISTS_READY));
  }

  private static class ShortlistQueryHandler extends AsyncQueryHandler {
    private static final int TOKEN_INIT_SHORTLIST = 1;
    private static final int TOKEN_INIT_PAIRS = 2;
    private static final int TOKEN_SHORTLIST = 3;
    private static final int TOKEN_SHORTLIST_ADDED = 4;

    private final WeakReference<ShortlistManager> shortlistManager;

    public ShortlistQueryHandler(WeakReference<ShortlistManager> shortlistManager,
                                 ContentResolver cr) {
      super(cr);
      this.shortlistManager = shortlistManager;
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, @Nullable Cursor cursor) {
      if (cursor == null) {
        Log.w(TAG, "onQueryComplete: null cursor");
        return;
      }
      try {
        switch (token) {
          case TOKEN_INIT_SHORTLIST:
            List<Shortlist> shortlists = new ArrayList<>();
            if (cursor.moveToFirst()) {
              int idColumn = cursor.getColumnIndexOrThrow(ShortlistsContract._ID);
              int nameColumn = cursor.getColumnIndexOrThrow(ShortlistsContract.NAME);
              do {
                shortlists.add(new Shortlist(cursor.getLong(idColumn),
                    cursor.getString(nameColumn)));
              } while (cursor.moveToNext());
            }
            ShortlistManager manager = shortlistManager.get();
            if (manager != null) {
              manager.setShortlists(shortlists);
            }
            break;
          case TOKEN_INIT_PAIRS:
            LongSparseArray<List<Shortlist>> shortlistMap = new LongSparseArray<>();
            manager = shortlistManager.get();
            if (manager == null) {
              return;
            }
            if (cursor.moveToFirst()) {
              int columnMediaId = cursor.getColumnIndexOrThrow(MediaShortlistContract
                  .MEDIA_ID);
              int columnShortlistId = cursor.getColumnIndexOrThrow(MediaShortlistContract
                  .SHORTLIST_ID);
              do {
                long mediaId = cursor.getLong(columnMediaId);
                shortlists = shortlistMap.get(mediaId);
                if (shortlists == null) {
                  shortlists = new ArrayList<>();
                  shortlistMap.put(mediaId, shortlists);
                }
                insert(shortlists, manager.shortlists.get(cursor.getLong(columnShortlistId)));
              } while (cursor.moveToNext());
            }
            manager.setShortlistPairs(shortlistMap);
            break;
          case TOKEN_SHORTLIST_ADDED:
            if (cursor.moveToFirst()) {
              long id = cursor.getLong(cursor.getColumnIndexOrThrow(ShortlistsContract
                  ._ID));
              String name = cursor.getString(cursor.getColumnIndexOrThrow
                  (ShortlistsContract
                      .NAME));
              Shortlist shortlist = new Shortlist(id, name);
              ShortlistManager shortlistManager = this.shortlistManager.get();
              if (shortlistManager != null) {
                shortlistManager.shortlists.put(shortlist.getId(), shortlist);
                shortlistManager.cachedShortlists = null;
                shortlistManager.notifyShortlistsChanged();
              }
            }
            break;
        }
      } finally {
        cursor.close();
      }
    }

    private void insert(List<Shortlist> shortlists, Shortlist shortlist) {
      long id = shortlist.getId();
      // Since we just created this shortlist, it probably belongs at
      // the end
      if (shortlists != null
          && (shortlists.size() == 0 || shortlists.get(0).getId() <
          id)) {
        shortlists.add(shortlist);
      } else if (shortlists != null) {
        int index = Collections.binarySearch(shortlists, shortlist);
        if (index >= 0) {
          shortlists.set(index, shortlist);
        } else {
          shortlists.add(-index - 1, shortlist);
        }
      }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, @NonNull Uri uri) {
      if (token == TOKEN_SHORTLIST) {
        startQuery(TOKEN_SHORTLIST_ADDED, cookie, uri, null, null, null, null);
      }
    }
  }
}
