package com.keyboardr.bluejay.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.keyboardr.bluejay.R;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import static android.content.ContentValues.TAG;

/**
 * Represents a piece of media to be played
 */
public class MediaItem implements Parcelable {
  public final CharSequence title;
  public final CharSequence artist;
  private final long albumId;
  private final long duration;
  private final String path;
  private final long transientMediaId;
  @Nullable
  public final Uri thumbnailUri;

  public static final LongSparseArray<Uri> thumbnailUriCache = new LongSparseArray<>();

  public static Builder build() {
    return new Builder();
  }

  @WorkerThread
  private MediaItem(CharSequence title, CharSequence artist, long albumId,
                    long duration, String path, long transientMediaId, @Nullable Uri thumbnailUri) {
    this.title = title;
    this.artist = artist;
    this.albumId = albumId;
    this.duration = duration;
    this.path = path;
    this.transientMediaId = transientMediaId;
    this.thumbnailUri = thumbnailUri;
  }

  @Nullable
  @WorkerThread
  private static Uri getThumbnailUri(Context context, long albumId) {
    if (albumId == -1) {
      return null;
    }

    Uri cached = thumbnailUriCache.get(albumId);
    if (cached != null) {
      return cached;
    }

    try (Cursor cursor = context.getContentResolver().query(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
        MediaStore.Audio.Albums._ID + "=?",
        new String[]{Long.toString(albumId)},
        null)) {
      if (cursor != null && cursor.moveToFirst()) {
        String artPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        if (artPath == null) {
          return null;
        }
        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(artPath).build();
        thumbnailUriCache.put(albumId, uri);
        return uri;
      }
    }
    return null;
  }

  @NonNull
  public Uri toUri() {
    return new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(path).build();
  }

  @WorkerThread
  @NonNull
  public Pair<Icon, Palette> getAlbumArt(@NonNull Context context) {
    if (thumbnailUri != null) {
      try {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver()
            , thumbnailUri);
        return new Pair<>(Icon.createWithBitmap(bitmap), Palette.from(bitmap).generate());
      } catch (IOException e) {
        Log.w(TAG, "getAlbumArt: no media found", e);
      }
    }
    return new Pair<>(Icon.createWithResource(context, R.drawable.album_art_empty), Palette.from
        (Collections.singletonList(
            new Palette.Swatch(context.getColor(R.color.colorPrimaryDark), 100))));
  }

  public long getDuration() {
    return duration;
  }

  public long getTransientId() {
    return transientMediaId;
  }

  public long getAlbumId() {
    return albumId;
  }

  @SuppressWarnings("WeakerAccess")
  public static class Builder {
    private CharSequence title;
    private CharSequence artist;
    private long albumId = -1;
    private long duration;
    private String path;

    public Builder setTitle(@Nullable CharSequence title) {
      this.title = title;
      return this;
    }

    public Builder setArtist(@Nullable CharSequence artist) {
      this.artist = artist;
      return this;
    }

    public Builder setAlbumId(long albumId) {
      this.albumId = albumId;
      return this;
    }

    public Builder setDuration(long duration) {
      this.duration = duration;
      return this;
    }

    public Builder setPath(@Nullable String path) {
      this.path = path;
      return this;
    }

    @WorkerThread
    public MediaItem make(Context context, long transientMediaId) {
      return make(transientMediaId, getThumbnailUri(context, albumId));
    }

    public MediaItem make(long transientMediaId, @Nullable Uri thumbnailUri) {
      return new MediaItem(title, artist, albumId, duration, path, transientMediaId, thumbnailUri);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    TextUtils.writeToParcel(this.title, dest, flags);
    TextUtils.writeToParcel(this.artist, dest, flags);
    dest.writeLong(this.albumId);
    dest.writeLong(this.duration);
    dest.writeString(this.path);
    dest.writeLong(this.transientMediaId);
    dest.writeParcelable(this.thumbnailUri, flags);
  }

  protected MediaItem(Parcel in) {
    this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    this.artist = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    this.albumId = in.readLong();
    this.duration = in.readLong();
    this.path = in.readString();
    this.transientMediaId = in.readLong();
    this.thumbnailUri = in.readParcelable(Uri.class.getClassLoader());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MediaItem mediaItem = (MediaItem) o;
    return albumId == mediaItem.albumId &&
        duration == mediaItem.duration &&
        transientMediaId == mediaItem.transientMediaId &&
        Objects.equals(title, mediaItem.title) &&
        Objects.equals(artist, mediaItem.artist) &&
        Objects.equals(path, mediaItem.path) &&
        Objects.equals(thumbnailUri, mediaItem.thumbnailUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, artist, albumId, duration, path, transientMediaId, thumbnailUri);
  }

  public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {
    @Override
    public MediaItem createFromParcel(Parcel source) {
      return new MediaItem(source);
    }

    @Override
    public MediaItem[] newArray(int size) {
      return new MediaItem[size];
    }
  };
}
