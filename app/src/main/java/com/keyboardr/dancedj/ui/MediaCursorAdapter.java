package com.keyboardr.dancedj.ui;

import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

/**
 * Created by keyboardr on 6/23/16.
 */

public class MediaCursorAdapter extends CursorRecyclerAdapter<MediaViewHolder> {

    private int artistColumn;
    private int titleColumn;

    public MediaCursorAdapter(@Nullable Cursor cursor) {
        super(cursor);
    }

    @Override
    protected void onNewCursor(@NonNull Cursor cursor) {
        artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
    }

    @Override
    protected void onBindViewHolder(MediaViewHolder viewHolder, @NonNull Cursor cursor) {
        viewHolder.bindMediaItem(MediaViewHolder.MediaItem.build()
                .setArtist(cursor.getString(artistColumn))
                .setTitle(cursor.getString(titleColumn))
                .make());
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaViewHolder(parent);
    }
}
