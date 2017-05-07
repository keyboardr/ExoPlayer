package com.keyboardr.bluejay.provider;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.model.Shortlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShortlistManager {

  public static final String ACTION_SHORTLISTS_READY = "com.keyboardr.bluejay.ui.monitor"
      + ".ShortlistManager.ACTION_SHORTLISTS_READY";

  public static final String ACTION_SHORTLISTS_CHANGED = "com.keyboardr.bluejay.ui.monitor"
      + "ShortlistManager.ACTION_SHORTLISTS_CHANGED";

  public static final String EXTRA_CHANGE_TYPE = "changeType";

  public static final String EXTRA_SHORTLIST = "shortlist";

  public static final String EXTRA_INDEX = "index";

  public static final String EXTRA_OLD_INDEX = "oldIndex";

  private static final String TAG = "ShortlistManager";

  private final Comparator<? super Shortlist> POSITION_COMPARATOR = new Comparator<Shortlist>
      () {
    @Override
    public int compare(Shortlist left, Shortlist right) {
      return positions.get(left.getId()) - positions.get(right.getId());
    }
  };

  @IntDef({Change.UNKNOWN, Change.ADD, Change.REMOVE, Change.MOVE, Change.RENAME})
  public @interface Change {
    int UNKNOWN = 0;
    int ADD = 1;
    int REMOVE = 2;
    int MOVE = 3;
    int RENAME = 4;
  }

  // Indexed by media id, contains shortlist IDs
  @Nullable
  private LongSparseArray<Set<Long>> shortlistMap;

  private List<Pair<MediaItem, Shortlist>> pendingAdds = new ArrayList<>();
  private List<Pair<MediaItem, Shortlist>> pendingRemoves = new ArrayList<>();
  private LongSparseArray<Shortlist> shortlists;
  private final List<Shortlist> positionedShortlists = new ArrayList<>();
  private final Map<Long, Integer> positions = new ArrayMap<>();

  private final AsyncQueryHandler queryHandler;
  private final LocalBroadcastManager localBroadcastManager;

  private int dirtyRangeBottom = -1;
  private int dirtyRangeTop = -1;

  private static volatile ShortlistManager instance;

  @NonNull
  public static ShortlistManager getInstance(Context context) {
    if (instance == null) {
      synchronized (ShortlistManager.class) {
        if (instance == null) {
          instance = new ShortlistManager(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private ShortlistManager(@NonNull Context context) {
    queryHandler = new ShortlistQueryHandler(context);
    localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
    queryHandler.startQuery(ShortlistQueryHandler.TOKEN_INIT_SHORTLIST, null,
        ShortlistsContract.CONTENT_URI, null, null, null, ShortlistsContract._ID);
    Log.d(TAG, "ShortlistManager: Init shortlist");
  }

  @Nullable
  public List<Shortlist> getShortlists(@NonNull MediaItem mediaItem) {
    if (shortlistMap == null) {
      return null;
    }
    Set<Long> shortlistIds = shortlistMap.get(mediaItem.getTransientId());
    if (shortlistIds == null) {
      shortlistIds = new ArraySet<>();
      shortlistMap.put(mediaItem.getTransientId(), shortlistIds);
    }

    List<Shortlist> result = new ArrayList<>();

    for (Long id : shortlistIds) {
      if (id == null) {
        Log.w(TAG, "getShortlists: null ID in shortlistIds");
        continue;
      }
      Shortlist shortlist = shortlists.get(id);
      if (shortlist == null) {
        shortlistIds.remove(id);
      } else {
        result.add(shortlist);
      }
    }
    Collections.sort(result, POSITION_COMPARATOR);
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

    if (shortlistMap == null) {
      pendingAdds.add(new Pair<>(mediaItem, shortlist));
      return;
    }
    Set<Long> shortlists = shortlistMap.get(mediaItem.getTransientId());

    if (!shortlists.contains(mediaItem.getTransientId())) {
      shortlists.add(shortlist.getId());
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

    if (shortlistMap == null) {
      pendingRemoves.add(new Pair<>(mediaItem, shortlist));
      return;
    }
    Set<Long> shortlists = shortlistMap.get(mediaItem.getTransientId());

    shortlists.remove(shortlist.getId());
    queryHandler.startDelete(0, null, MediaShortlistContract.CONTENT_URI,
        MediaShortlistContract.SHORTLIST_ID + " = ? AND "
            + MediaShortlistContract.MEDIA_ID + " = ?",
        new String[]{shortlist.getId() + "", mediaItem.getTransientId() + ""});
  }

  public List<Shortlist> getShortlists() {
    return Collections.unmodifiableList(positionedShortlists);
  }

  public boolean isInShortlist(@NonNull MediaItem mediaItem, @NonNull Shortlist shortlist) {
    if (shortlistMap == null) {
      return false;
    }
    Set<Long> shortlists = shortlistMap.get(mediaItem.getTransientId());
    return shortlists != null && shortlists.contains(shortlist.getId());
  }

  public void createShortlist(@NonNull String name) {
    if (shortlists == null) {
      throw new IllegalStateException("Shortlists not yet initialized");
    }
    ContentValues values = new ContentValues();
    values.put(ShortlistsContract.NAME, name);
    values.put(ShortlistsContract.POSITION, getShortlists().size());
    queryHandler.startInsert(ShortlistQueryHandler.TOKEN_SHORTLIST, null,
        ShortlistsContract.CONTENT_URI, values);
  }

  public void deleteShortlist(@NonNull Shortlist shortlist) {
    if (shortlists == null) {
      throw new IllegalStateException("Shortlists not yet initialized");
    }
    shortlists.remove(shortlist.getId());
    int index = -1;
    for (int i = positionedShortlists.size() - 1; i >= 0; i--) {
      long listShortlistId = positionedShortlists.get(i).getId();
      if (listShortlistId == shortlist.getId()) {
        positionedShortlists.remove(i);
        positions.remove(shortlist.getId());
        dirtyRangeBottom = Math.min(dirtyRangeBottom, i);
        dirtyRangeTop = Math.max(dirtyRangeTop, positionedShortlists.size());
        index = i;
        break;
      } else {
        positions.put(listShortlistId, i - 1);
      }
    }

    queryHandler.startDelete(0, null, ContentUris.withAppendedId(ShortlistsContract.CONTENT_URI,
        shortlist.getId()), null, null);
    queryHandler.startDelete(0, null, MediaShortlistContract.CONTENT_URI,
        MediaShortlistContract.SHORTLIST_ID + " = " + shortlist.getId(), null);
    queueUpdatePositions(500);
    notifyShortlistsChanged(Change.REMOVE, shortlist, -1, index);
  }

  public void moveShortlist(int oldIndex, int newIndex) {
    Shortlist shortlist = positionedShortlists.remove(oldIndex);
    positionedShortlists.add(newIndex, shortlist);

    if (dirtyRangeTop == -1) {
      dirtyRangeTop = oldIndex;
      dirtyRangeBottom = oldIndex;
    } else {
      dirtyRangeTop = Math.max(dirtyRangeTop, oldIndex);
      dirtyRangeBottom = Math.min(dirtyRangeBottom, oldIndex);
    }

    dirtyRangeTop = Math.max(dirtyRangeTop, newIndex);
    dirtyRangeBottom = Math.min(dirtyRangeBottom, newIndex);

    queueUpdatePositions(5000);
    notifyShortlistsChanged(Change.MOVE, shortlist, oldIndex, newIndex);
  }

  private void queueUpdatePositions(long delay) {
    queryHandler.removeMessages(ShortlistQueryHandler.MESSAGE_UPDATE_POSITIONS);
    queryHandler.sendEmptyMessageDelayed(ShortlistQueryHandler.MESSAGE_UPDATE_POSITIONS, delay);
  }

  public void renameShortlist(@NonNull Shortlist shortlist, @NonNull String name) {
    Shortlist newShortlist = new Shortlist(shortlist.getId(), name);
    shortlists.put(shortlist.getId(), newShortlist);
    int position = positions.get(shortlist.getId());
    positionedShortlists.set(position, newShortlist);
    ContentValues values = new ContentValues();
    values.put(ShortlistsContract.NAME, name);
    queryHandler.startUpdate(0, null, ContentUris.withAppendedId(ShortlistsContract.CONTENT_URI,
        shortlist.getId()), values, null, null);
    notifyShortlistsChanged(Change.RENAME, newShortlist, position, position);
  }

  public boolean isReady() {
    return shortlists != null && shortlistMap != null;
  }

  private void notifyShortlistsChanged(@Change int changeType, @Nullable Shortlist shortlist, int
      index, int oldIndex) {
    Intent intent = new Intent(ACTION_SHORTLISTS_CHANGED);
    intent.putExtra(EXTRA_CHANGE_TYPE, changeType);
    intent.putExtra(EXTRA_SHORTLIST, shortlist);
    intent.putExtra(EXTRA_INDEX, index);
    intent.putExtra(EXTRA_OLD_INDEX, oldIndex);
    localBroadcastManager.sendBroadcast(intent);
  }

  private void setShortlists(@NonNull List<Shortlist> shortlists) {
    this.shortlists = new LongSparseArray<>(shortlists.size());
    for (Shortlist shortlist : shortlists) {
      this.shortlists.put(shortlist.getId(), shortlist);
      positionedShortlists.add(shortlist);
    }
    Collections.sort(positionedShortlists, POSITION_COMPARATOR);
    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLISTS_CHANGED));
    Log.d(TAG, "setShortlists: init pairs");
    queryHandler.startQuery(ShortlistQueryHandler.TOKEN_INIT_PAIRS, null,
        MediaShortlistContract.CONTENT_URI, null, null, null,
        MediaShortlistContract.SHORTLIST_ID);
  }

  private void setShortlistPairs(LongSparseArray<Set<Long>> shortlistMap) {
    this.shortlistMap = shortlistMap;

    for (Pair<MediaItem, Shortlist> add : pendingAdds) {
      add(add.first, add.second);
    }
    for (Pair<MediaItem, Shortlist> remove : pendingRemoves) {
      remove(remove.first, remove.second);
    }

    localBroadcastManager.sendBroadcast(new Intent(ACTION_SHORTLISTS_READY));
  }

  private static class ShortlistQueryHandler extends AsyncQueryHandler {
    private static final int TOKEN_INIT_SHORTLIST = 1;
    private static final int TOKEN_INIT_PAIRS = 2;
    private static final int TOKEN_SHORTLIST = 3;
    private static final int TOKEN_SHORTLIST_ADDED = 4;
    private static final int MESSAGE_UPDATE_POSITIONS = 5;

    private final Context context;

    public ShortlistQueryHandler(Context context) {
      super(context.getApplicationContext().getContentResolver());
      this.context = context.getApplicationContext();
    }

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == MESSAGE_UPDATE_POSITIONS) {
        ShortlistManager manager = getInstance(context);
        int top = clamp(manager.dirtyRangeTop, manager.positionedShortlists.size() - 1, 0);
        int bottom = clamp(manager.dirtyRangeBottom, manager.positionedShortlists.size() - 1, 0);
        manager.dirtyRangeTop = manager.dirtyRangeBottom = -1;

        for (int i = bottom; i <= top; i++) {
          Shortlist shortlist = manager.positionedShortlists.get(i);
          manager.positions.put(shortlist.getId(), i);
          ContentValues values = new ContentValues();
          values.put(ShortlistsContract.POSITION, i);
          startUpdate(0, null,
              ContentUris.withAppendedId(ShortlistsContract.CONTENT_URI, shortlist.getId()),
              values, null, null);
        }
      } else {
        super.handleMessage(msg);
      }
    }

    private static int clamp(int value, int max, int min) {
      return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, @Nullable final Cursor cursor) {
      if (cursor == null) {
        Log.w(TAG, "onQueryComplete: null cursor");
        return;
      }
      final ShortlistManager manager = getInstance(context);
      boolean isAsync = false;
      try {
        switch (token) {
          case TOKEN_INIT_SHORTLIST:
            Log.d(TAG, "onQueryComplete: Finished load shortlists");
            List<Shortlist> shortlists = new ArrayList<>();
            if (cursor.moveToFirst()) {
              int idColumn = cursor.getColumnIndexOrThrow(ShortlistsContract._ID);
              int nameColumn = cursor.getColumnIndexOrThrow(ShortlistsContract.NAME);
              int posColumn = cursor.getColumnIndexOrThrow(ShortlistsContract.POSITION);
              do {
                Shortlist shortlist = new Shortlist(cursor.getLong(idColumn),
                    cursor.getString(nameColumn));
                shortlists.add(shortlist);
                manager.positions.put(shortlist.getId(), cursor.getInt(posColumn));
              } while (cursor.moveToNext());
            }
            manager.setShortlists(shortlists);
            Log.d(TAG, "onQueryComplete: Finished init shortlists");
            break;
          case TOKEN_INIT_PAIRS:
            Log.d(TAG, "onQueryComplete: Finished load pairs");
            if (cursor.moveToFirst()) {
              isAsync = true;
              new AsyncTask<Cursor, Void, LongSparseArray<Set<Long>>>() {

                @Override
                protected LongSparseArray<Set<Long>> doInBackground(Cursor... cursors) {
                  try {
                    LongSparseArray<Set<Long>> shortlistMap = new LongSparseArray<>();
                    Cursor cursor = cursors[0];
                    int columnMediaId = cursor.getColumnIndexOrThrow(MediaShortlistContract
                        .MEDIA_ID);
                    int columnShortlistId = cursor.getColumnIndexOrThrow(MediaShortlistContract
                        .SHORTLIST_ID);
                    do {
                      long mediaId = cursor.getLong(columnMediaId);
                      Set<Long> shortlistIds = shortlistMap.get(mediaId);
                      if (shortlistIds == null) {
                        shortlistIds = new ArraySet<>();
                        shortlistMap.put(mediaId, shortlistIds);
                      }
                      shortlistIds.add(cursor.getLong(columnShortlistId));
                    } while (cursor.moveToNext());
                    return shortlistMap;
                  } finally {
                    cursor.close();
                  }
                }

                @Override
                protected void onPostExecute(LongSparseArray<Set<Long>> result) {
                  manager.setShortlistPairs(result);
                  Log.d(TAG, "onQueryComplete: Finished init pairs");
                }
              }.execute(cursor);
            } else {
              // No pairs
              manager.setShortlistPairs(new LongSparseArray<Set<Long>>());
            }
            break;
          case TOKEN_SHORTLIST_ADDED:
            if (cursor.moveToFirst()) {
              long id = cursor.getLong(cursor.getColumnIndexOrThrow(ShortlistsContract
                  ._ID));
              String name = cursor.getString(cursor.getColumnIndexOrThrow
                  (ShortlistsContract
                      .NAME));
              int position = cursor.getInt(cursor.getColumnIndexOrThrow(ShortlistsContract
                  .POSITION));
              Shortlist shortlist = new Shortlist(id, name);
              manager.shortlists.put(shortlist.getId(), shortlist);
              manager.positions.put(shortlist.getId(), position);
              manager.positionedShortlists.add(position, shortlist);
              if (manager.dirtyRangeTop >= 0) {
                manager.dirtyRangeTop = Math.max(manager.dirtyRangeTop, position);
              }
              manager.notifyShortlistsChanged(Change.ADD, shortlist, position, -1);
            }
            break;
        }
      } finally {
        if (!isAsync) {
          cursor.close();
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
