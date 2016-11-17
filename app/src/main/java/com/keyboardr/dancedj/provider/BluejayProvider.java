package com.keyboardr.dancedj.provider;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.tjeannin.provigen.ProviGenOpenHelper;
import com.tjeannin.provigen.ProviGenProvider;

public class BluejayProvider extends ProviGenProvider {
    private static final String DB_NAME = "bluejaydb";
    private static final Class[] CLASSES = {};

    @Override
    public SQLiteOpenHelper openHelper(Context context) {
        return new ProviGenOpenHelper(context, DB_NAME, null, 1, CLASSES);
    }

    @Override
    public Class[] contractClasses() {
        return CLASSES;
    }
}
