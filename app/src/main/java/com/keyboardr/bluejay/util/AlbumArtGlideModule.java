package com.keyboardr.bluejay.util;

import android.content.Context;
import android.support.annotation.Keep;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.GlideModule;
import com.keyboardr.bluejay.model.MediaItem;

import java.io.InputStream;

/**
 * Registers Glide components for resolving AlbumArt
 */
@Keep
public class AlbumArtGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
  }

  @Override
  public void registerComponents(Context context, Glide glide) {
    glide.register(MediaItem.class, InputStream.class, AlbumArtUriLoader.FACTORY);
  }
}
