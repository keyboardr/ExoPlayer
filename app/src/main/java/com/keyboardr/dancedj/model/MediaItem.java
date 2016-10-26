package com.keyboardr.dancedj.model;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Represents a piece of media to be played
 */
public class MediaItem {
    public final CharSequence title;
    public final CharSequence artist;
    @SuppressWarnings("WeakerAccess")
    public final String path;

    public static Builder build() {
        return new Builder();
    }

    private MediaItem(CharSequence title, CharSequence artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    @NonNull
    public Uri toUri() {
       return new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(path).build();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private CharSequence title;
        private CharSequence artist;
        private String path;

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setArtist(CharSequence artist) {
            this.artist = artist;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public MediaItem make() {
            return new MediaItem(title, artist, path);
        }
    }
}
