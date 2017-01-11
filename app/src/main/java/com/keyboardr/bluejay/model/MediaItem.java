package com.keyboardr.bluejay.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

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
        return new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(artPath).build();
      }
    }
    return null;
  }

  @NonNull
  public Uri toUri() {
    return new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(path).build();
  }

  @WorkerThread
  @Nullable
  public Bitmap getAlbumArt(@NonNull Context context) {
    if (thumbnailUri == null) {
      return null;
    }
    try {
      return MediaStore.Images.Media.getBitmap(context.getContentResolver(), thumbnailUri);
    } catch (IOException e) {
      Log.w(TAG, "getAlbumArt: no media found", e);
      return null;
    }
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
