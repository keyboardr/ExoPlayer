package com.keyboardr.bluejay.provider;

import android.net.Uri;

import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

public interface ShortlistsContract extends ProviGenBaseContract {
  String TABLE = "shortlists";

  @ContentUri
  Uri CONTENT_URI = BluejayProvider.generateContentUri(TABLE);

  @Column(Column.Type.TEXT)
  String NAME = "name";

  @Column(Column.Type.INTEGER)
  String POSITION = "position";
}
