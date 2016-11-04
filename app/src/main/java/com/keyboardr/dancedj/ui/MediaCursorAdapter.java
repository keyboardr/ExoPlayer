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
    private int albumIdColumn;
    private int dataColumn;
    private int mediaIdColumn;

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
        albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
        dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        mediaIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
    }

    @Override
    protected void onBindViewHolder(MediaViewHolder viewHolder, @NonNull Cursor cursor) {
        viewHolder.bindMediaItem(MediaItem.build()
                .setArtist(cursor.getString(artistColumn))
                .setTitle(cursor.getString(titleColumn))
                .setAlbumId(cursor.getLong(albumIdColumn))
                .setPath(cursor.getString(dataColumn))
                .make(cursor.getLong(mediaIdColumn)));
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaViewHolder(parent, mediaItemSelectedListener);
    }
}
