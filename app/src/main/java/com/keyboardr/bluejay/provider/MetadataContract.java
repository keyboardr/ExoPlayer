package com.keyboardr.bluejay.provider;

import android.net.Uri;

import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

/**
 * Created by Keyboardr on 11/16/2016.
 */

public interface MetadataContract extends ProviGenBaseContract {
    String TABLE = "metadata";

    @ContentUri
    Uri CONTENT_URI = BluejayProvider.generateContentUri(TABLE);

    @Column(Column.Type.INTEGER)
    String START_OFFSET = "start_offset";

    @Column(Column.Type.INTEGER)
    String END_OFFSET = "end_offset";

    @Column(Column.Type.INTEGER)
    String KEY_ROOT = "key_root";

}
