package com.keyboardr.dancedj.model;

/**
 * Created by keyboardr on 6/23/16.
 */
public class MediaItem {
    public final CharSequence title;
    public final CharSequence artist;
    public final String path;

    public static Builder build() {
        return new Builder();
    }

    private MediaItem(CharSequence title, CharSequence artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

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
