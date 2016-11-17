package com.keyboardr.bluejay.ui.recycler;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.provider.BaseColumns;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * Created by keyboardr on 6/23/16.
 */

public abstract class CursorRecyclerAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<T> {

    @NonNull
    private final DataSetObserver dataSetObserver;

    @Nullable
    private Cursor cursor;
    private boolean dataValid;
    private int rowIdColumn;

    public CursorRecyclerAdapter(@Nullable Cursor cursor) {
        dataSetObserver = new NotifyingDataSetObserver();
        setCursor(cursor);
        setHasStableIds(true);
    }

    @Nullable
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public int getItemCount() {
        return dataValid && cursor != null ? cursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (dataValid && cursor != null && cursor.moveToPosition(position)) {
            return cursor.getLong(rowIdColumn);
        }
        return 0;
    }

    protected abstract void onBindViewHolder(T viewHolder, @NonNull Cursor cursor);

    @Override
    public void onBindViewHolder(T viewHolder, int position) {
        if (!dataValid || cursor == null) {
            throw new IllegalStateException("Can only bind when data is valid");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("Couldn't move to cursor position: " + position);
        }
        onBindViewHolder(viewHolder, cursor);
    }

    public void changeCursor(@Nullable Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Cursor swapCursor(@Nullable Cursor newCursor) {
        if (newCursor == cursor) {
            return null;
        }
        final Cursor oldCursor = cursor;
        if (oldCursor != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        setCursor(newCursor);
        notifyDataSetChanged();
        return oldCursor;
    }

    @CallSuper
    private void setCursor(@Nullable Cursor newCursor) {
        cursor = newCursor;
        if (cursor != null) {
            cursor.registerDataSetObserver(dataSetObserver);
            rowIdColumn = newCursor.getColumnIndexOrThrow(BaseColumns._ID);
            dataValid = true;
            onNewCursor(cursor);
        } else {
            rowIdColumn = -1;
            dataValid = false;
        }
    }

    protected abstract void onNewCursor(@NonNull Cursor cursor);

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            dataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            dataValid = false;
            notifyDataSetChanged();
        }
    }
}
