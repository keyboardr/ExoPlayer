package com.keyboardr.bluejay.ui.recycler;

import android.graphics.Typeface;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.util.MathUtil;

/**
 * ViewHolder for media items
 */
public class MediaViewHolder extends RecyclerView.ViewHolder {

  public interface MediaViewDecorator {
    void onMediaItemSelected(MediaItem mediaItem);

    @DrawableRes
    int getIconForItem(MediaItem mediaItem);

    void onDecoratorSelected(MediaItem mediaItem);

    boolean showMoreOption();

    void onMoreSelected(MediaItem mediaItem);

  }

  private final TextView title;
  private final TextView artist;
  private final TextView duration;
  private final ImageView icon;
  private final ImageView menu;

  private MediaItem mediaItem;

  private final MediaViewDecorator mediaViewDecorator;

  public MediaViewHolder(@NonNull ViewGroup parent,
                         @Nullable final MediaViewDecorator mediaViewDecorator) {
    super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media,
        parent, false));
    title = (TextView) itemView.findViewById(R.id.media_item_title);
    artist = (TextView) itemView.findViewById(R.id.media_item_artist);
    duration = (TextView) itemView.findViewById(R.id.media_item_duration);
    icon = ((ImageView) itemView.findViewById(R.id.media_item_icon));
    menu = ((ImageView) itemView.findViewById(R.id.media_item_menu));

    this.mediaViewDecorator = mediaViewDecorator;

    if (mediaViewDecorator != null) {
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mediaViewDecorator.onMediaItemSelected(mediaItem);
        }
      });

      icon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mediaViewDecorator.onDecoratorSelected(mediaItem);
        }
      });

      menu.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mediaViewDecorator.onMoreSelected(mediaItem);
        }
      });

    }
  }

  public void bindMediaItem(@NonNull MediaItem mediaItem, boolean selected, boolean enabled) {
    itemView.setSelected(selected);
    itemView.setEnabled(enabled);
    SpannableString titleString = new SpannableString(mediaItem.title);
    titleString.setSpan(new StyleSpan(enabled
            ? (selected ? Typeface.BOLD : Typeface.NORMAL)
            : Typeface.ITALIC),
        0, titleString.length(), 0);
    title.setText(titleString);
    artist.setText(mediaItem.artist);

    duration.setText(MathUtil.getSongDuration(mediaItem.getDuration()));

    this.mediaItem = mediaItem;

    if (mediaViewDecorator != null) {
      int iconForItem = mediaViewDecorator.getIconForItem(mediaItem);
      if (iconForItem <= 0) {
        icon.setVisibility(View.GONE);
      } else {
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(iconForItem);
      }

      if (mediaViewDecorator.showMoreOption()) {
        menu.setVisibility(View.VISIBLE);
      } else {
        menu.setVisibility(View.INVISIBLE);
      }
    } else {
      icon.setVisibility(View.GONE);
      menu.setVisibility(View.INVISIBLE);
    }
  }
}
