package com.keyboardr.bluejay.provider;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.keyboardr.bluejay.BuildConfig;
import com.tjeannin.provigen.ProviGenOpenHelper;
import com.tjeannin.provigen.ProviGenProvider;
import com.tjeannin.provigen.model.Contract;

public class BluejayProvider extends ProviGenProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    private static final String DB_NAME = "bluejaydb";
    private static final Class[] CLASSES = {ShortlistsContract.class, MediaShortlistContract.class,
            MetadataContract.class};

    @Override
    public SQLiteOpenHelper openHelper(Context context) {
        return new ProviGenOpenHelper(context, DB_NAME, null, 1, CLASSES);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = openHelper.getReadableDatabase();

        Contract contract = findMatchingContract(uri);
        Cursor cursor;

        String table = contract.getTable();
        table = swapQueryTableIfNecessary(table);
        switch (uriMatcher.match(uri)) {
            case ITEM:
                cursor = database.query(table, projection, selection, selectionArgs, "", "", sortOrder);
                break;
            case ITEM_ID:
                String itemId = String.valueOf(ContentUris.parseId(uri));
                if (TextUtils.isEmpty(selection)) {
                    cursor = database.query(table, projection, contract.getIdField() + " = ? ", new String[]{itemId}, "", "", sortOrder);
                } else {
                    cursor = database.query(table, projection, selection + " AND " + contract.getIdField() + " = ? ",
                            appendToStringArray(selectionArgs, itemId), "", "", sortOrder);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }

        // Make sure that potential listeners are getting notified.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Class[] contractClasses() {
        return CLASSES;
    }

    @NonNull
    private String swapQueryTableIfNecessary(@NonNull String table) {
        switch (table) {
            case MediaShortlistContract.TABLE:
                return MediaShortlistContract.TABLE + " LEFT JOIN "
                        + ShortlistsContract.TABLE + " ON "
                        + MediaShortlistContract.MEDIA_ID + " = " +
                            ShortlistsContract.TABLE + "." + ShortlistsContract._ID;
            default:
                return table;
        }
    }
}
