package com.keyboardr.dancedj.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * ViewHolder for media items
 */
public class MediaViewHolder extends RecyclerView.ViewHolder {
    public static class MediaItem {
        private final CharSequence title;
        private final CharSequence artist;

        public static Builder build() {
            return new Builder();
        }

        private MediaItem(CharSequence title, CharSequence artist) {
            this.title = title;
            this.artist = artist;
        }

        public static class Builder {
            private CharSequence title;
            private CharSequence artist;

            public Builder setTitle(CharSequence title) {
                this.title = title;
                return this;
            }

            public Builder setArtist(CharSequence artist) {
                this.artist = artist;
                return this;
            }

            public MediaItem make() {
                return new MediaItem(title, artist);
            }
        }
    }

    private final TextView title;
    private final TextView artist;

    private MediaItem mediaItem;

    public MediaViewHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item,
                parent, false));
        title = (TextView) itemView.findViewById(android.R.id.text1);
        artist = (TextView) itemView.findViewById(android.R.id.text2);
    }

    public void bindMediaItem(@NonNull MediaItem mediaItem) {
        title.setText(mediaItem.title);
        artist.setText(mediaItem.artist);
    }
}
