package com.keyboardr.bluejay.provider;

import android.content.ContentResolver;
import android.net.Uri;

import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

/**
 * Created by Keyboardr on 11/16/2016.
 */

public interface MediaShortlistContract extends ProviGenBaseContract {
    String TABLE = "media_shortlist";

    @ContentUri
    Uri CONTENT_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" +
            BluejayProvider.AUTHORITY + "/" + TABLE);

    @Column(Column.Type.INTEGER)
    String SHORTLIST_ID = "shortlist_id";

    @Column(Column.Type.INTEGER)
    String MEDIA_ID = "media_id";

}
