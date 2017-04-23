package com.keyboardr.bluejay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.keyboardr.bluejay.model.MediaItem;

import java.io.InputStream;

/**
 * Loads the thumbnailUri for a MediaItem
 */

public class AlbumArtUriLoader implements ModelLoader<MediaItem, InputStream> {

  public static ModelLoaderFactory<MediaItem, InputStream> FACTORY
      = new ModelLoaderFactory<MediaItem, InputStream>() {

    @Override
    public ModelLoader<MediaItem, InputStream> build(Context context,
                                                     GenericLoaderFactory factories) {
      return new AlbumArtUriLoader(context.getContentResolver(),
          factories.buildModelLoader(Uri.class, InputStream.class));
    }

    @Override
    public void teardown() {
    }
  };

  private final ContentResolver contentResolver;
  private final ModelLoader<Uri, InputStream> streamLoader;

  private AlbumArtUriLoader(@NonNull ContentResolver contentResolver,
                            ModelLoader<Uri, InputStream> streamLoader) {
    this.contentResolver = contentResolver;
    this.streamLoader = streamLoader;
  }

  @Override
  public DataFetcher<InputStream> getResourceFetcher(MediaItem model, int width, int height) {
    return new AlbumArtUriFetcher(model, width, height, contentResolver, streamLoader);
  }

  private static class AlbumArtUriFetcher implements DataFetcher<InputStream> {

    private final MediaItem mediaItem;
    private final int width;
    private final int height;
    private final ContentResolver contentResolver;
    private final ModelLoader<Uri, InputStream> streamLoader;

    public AlbumArtUriFetcher(MediaItem mediaItem, int width, int height,
                              ContentResolver contentResolver,
                              ModelLoader<Uri, InputStream> streamLoader) {
      this.mediaItem = mediaItem;
      this.width = width;
      this.height = height;
      this.contentResolver = contentResolver;
      this.streamLoader = streamLoader;
    }

    @Nullable
    @Override
    public InputStream loadData(Priority priority) throws Exception {
      long albumId = mediaItem.getAlbumId();
      if (albumId == -1) {
        return null;
      }

      try (Cursor cursor = contentResolver.query(
          MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
          new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
          MediaStore.Audio.Albums._ID + "=?",
          new String[]{Long.toString(albumId)},
          null)) {
        if (cursor != null && cursor.moveToFirst()) {
          String artPath = cursor.getString(
              cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
          if (artPath == null) {
            return null;
          }
          return streamLoader.getResourceFetcher(new Uri.Builder().scheme(
              ContentResolver.SCHEME_FILE).path(artPath)
              .build(), width, height).loadData(priority);
        }
      }
      return null;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String getId() {
      return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.toString() + ":" + mediaItem.getAlbumId();
    }

    @Override
    public void cancel() {
    }


  }
}
