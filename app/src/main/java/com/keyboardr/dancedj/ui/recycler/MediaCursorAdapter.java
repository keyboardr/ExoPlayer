package com.keyboardr.dancedj.ui.recycler;

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
    private int durationColumn;
    private int dataColumn;
    private int mediaIdColumn;

    @Nullable
    private final MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener;

    @Nullable
    private final MediaViewHolder.MediaViewDecorator mediaViewDecorator;

    public MediaCursorAdapter(@Nullable Cursor cursor,
                              @Nullable MediaViewHolder.OnMediaItemSelectedListener mediaItemSelectedListener,
                              @Nullable MediaViewHolder.MediaViewDecorator mediaViewDecorator) {
        super(cursor);
        this.mediaItemSelectedListener = mediaItemSelectedListener;
        this.mediaViewDecorator = mediaViewDecorator;
    }

    @Override
    protected void onNewCursor(@NonNull Cursor cursor) {
        artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
        durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
        dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        mediaIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
    }

    @Override
    protected void onBindViewHolder(MediaViewHolder viewHolder, @NonNull Cursor cursor) {
        viewHolder.bindMediaItem(MediaItem.build()
                .setArtist(cursor.getString(artistColumn))
                .setTitle(cursor.getString(titleColumn))
                .setAlbumId(cursor.getLong(albumIdColumn))
                .setDuration(cursor.getLong(durationColumn))
                .setPath(cursor.getString(dataColumn))
                .make(cursor.getLong(mediaIdColumn)), false, true);
    }

    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaViewHolder(parent, mediaItemSelectedListener, mediaViewDecorator);
    }
}
