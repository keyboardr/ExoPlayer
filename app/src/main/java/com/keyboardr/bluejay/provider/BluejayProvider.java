package com.keyboardr.bluejay.provider;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.keyboardr.bluejay.BuildConfig;
import com.tjeannin.provigen.ProviGenOpenHelper;
import com.tjeannin.provigen.ProviGenProvider;

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
    public Class[] contractClasses() {
        return CLASSES;
    }
}
