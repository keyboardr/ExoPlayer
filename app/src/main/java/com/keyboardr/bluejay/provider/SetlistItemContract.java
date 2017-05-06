package com.keyboardr.bluejay.provider;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ShareCompat;
import android.support.v4.util.Pair;
import android.text.Html;

import com.jrummyapps.android.util.HtmlBuilder;
import com.keyboardr.bluejay.R;
import com.tjeannin.provigen.ProviGenBaseContract;
import com.tjeannin.provigen.annotation.Column;
import com.tjeannin.provigen.annotation.ContentUri;

public interface SetlistItemContract extends ProviGenBaseContract {
  class Utils {

    public static void shareSetlist(@NonNull final Activity activity,
                                    final @NonNull String name, final long id) {
      new AsyncTask<Void, Void, Pair<String, String>>() {
        @Override
        protected Pair<String, String> doInBackground(Void... params) {
          try (Cursor cursor = activity.getContentResolver().query(
              CONTENT_URI, new String[]{ARTIST, TITLE},
              SETLIST_ID + "=?", new String[]{Long.toString(id)},
              POSITION)) {
            StringBuilder plain = new StringBuilder(name).append('\n');
            HtmlBuilder html = new HtmlBuilder().h4(name);
            if (cursor != null && cursor.moveToFirst()) {
              int artistColumn = cursor.getColumnIndexOrThrow(ARTIST);
              int titleColumn = cursor.getColumnIndexOrThrow(TITLE);
              html.open("ol");
              do {
                String song = cursor.getString(titleColumn) + " - " + cursor.getString(
                    artistColumn);
                plain.append(song).append('\n');
                //noinspection NewApi
                html.li(Html.escapeHtml(song));
              } while (cursor.moveToNext());
              plain.deleteCharAt(plain.length() - 1);
              html.close();
            }
            return new Pair<>(plain.toString(), html.toString());
          }
        }

        @Override
        protected void onPostExecute(Pair<String, String> result) {
          ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(activity);
          builder.setText(result.first);
          builder.setHtmlText(result.second);
          builder.setSubject(name);
          builder.setChooserTitle(activity.getString(R.string.share_formatted, name));
          builder.setType("text/plain");
          builder.startChooser();
        }
      }.execute();
    }
  }

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

  @Column(Column.Type.INTEGER)
  String DURATION = "duration";
}
