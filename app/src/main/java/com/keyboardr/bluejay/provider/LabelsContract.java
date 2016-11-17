package com.keyboardr.bluejay.provider;

import android.content.ContentResolver;
import android.net.Uri;

import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

public interface LabelsContract extends ProviGenBaseContract {
    String TABLE = "label";

    @ContentUri
    Uri CONTENT_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + BluejayProvider
            .AUTHORITY + "/" + TABLE);

    @Column(Column.Type.TEXT)
    String NAME = "name";
}
