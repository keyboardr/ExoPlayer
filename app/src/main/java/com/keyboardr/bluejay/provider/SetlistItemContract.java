package com.keyboardr.bluejay.provider;

import android.net.Uri;

import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

public interface SetlistItemContract extends ProviGenBaseContract {
  String TABLE = "setlist_items";

  @ContentUri
  Uri CONTENT_URI = BluejayProvider.generateContentUri(TABLE);

  @Column(Column.Type.INTEGER)
  String SETLIST_ID = "shortlist_id";

  @Column(Column.Type.INTEGER)
  String MEDIA_ID = "media_id";

  @Column(Column.Type.INTEGER)
  String POSITION = "position";

  @Column(Column.Type.TEXT)
  String TITLE = "title";

  @Column(Column.Type.TEXT)
  String ARTIST = "artist";

}
