package com.keyboardr.dancedj.ui;

import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.keyboardr.dancedj.model.MediaItem;

/**
 * Created by keyboardr on 6/23/16.
 */

public class MediaCursorAdapter extends CursorRecyclerAdapter<MediaViewHolder> {

    private int artistColumn;
    private int titleColumn;
    private int dataColumn;

    @Nullable
    private final MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener;

    public MediaCursorAdapter(@Nullable Cursor cursor, @Nullable MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener) {
        super(cursor);
        this.mediaItemSelectedListener = mediaItemSelectedListener;
    }

    @Override
    protected void onNewCursor(@NonNull Cursor cursor) {
        artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
    }

    @Override
    protected void onBindViewHolder(MediaViewHolder viewHolder, @NonNull Cursor cursor) {
        viewHolder.bindMediaItem(MediaItem.build()
                .setArtist(cursor.getString(artistColumn))
                .setTitle(cursor.getString(titleColumn))
                .setPath(cursor.getString(dataColumn))
                .make());
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaViewHolder(parent, mediaItemSelectedListener);
    }
}
