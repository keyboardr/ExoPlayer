package com.keyboardr.bluejay.ui.recycler;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.util.MathUtil;

/**
 * ViewHolder for media items
 */
public class MediaViewHolder extends RecyclerView.ViewHolder {

  public interface MediaViewDecorator {
    void onMediaItemSelected(@NonNull MediaItem mediaItem);

    @DrawableRes
    int getIconForItem(@NonNull MediaItem mediaItem);

    void onDecoratorSelected(@NonNull MediaItem mediaItem, @NonNull View view);

    boolean showMoreOption();

    void onMoreSelected(@NonNull MediaItem mediaItem);

  }

  public interface DragStartListener {
    void startDrag(@NonNull MediaViewHolder viewHolder);

    boolean canDrag(@NonNull MediaItem mediaItem, boolean selected, boolean enabled);

    boolean canClick(@NonNull MediaItem mediaItem);

    void onMediaItemClicked(@NonNull MediaItem mediaItem);
  }

  private final TextView title;
  private final TextView subText;
  private final ImageView icon;
  private final ImageView menu;
  private final ImageView albumArt;

  private MediaItem mediaItem;

  private final MediaViewDecorator mediaViewDecorator;
  private final DragStartListener dragStartListener;

  public MediaViewHolder(@NonNull ViewGroup parent,
                         @Nullable MediaViewDecorator mediaViewDecorator) {
    this(parent, mediaViewDecorator, null);
  }

  public MediaViewHolder(@NonNull ViewGroup parent,
                         @Nullable DragStartListener dragStartListener) {
    this(parent, null, dragStartListener);
  }

  private MediaViewHolder(@NonNull ViewGroup parent,
                          @Nullable final MediaViewDecorator mediaViewDecorator,
                          @Nullable final DragStartListener dragStartListener) {
    super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media,
        parent, false));
    title = (TextView) itemView.findViewById(R.id.media_item_title);
    subText = (TextView) itemView.findViewById(R.id.media_item_subtext);
    icon = (ImageView) itemView.findViewById(R.id.media_item_icon);
    menu = (ImageView) itemView.findViewById(R.id.media_item_menu);
    albumArt = (ImageView) itemView.findViewById(R.id.media_item_album_art);

    this.mediaViewDecorator = mediaViewDecorator;
    this.dragStartListener = dragStartListener;

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
          mediaViewDecorator.onDecoratorSelected(mediaItem, itemView);
        }
      });

      menu.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          mediaViewDecorator.onMoreSelected(mediaItem);
        }
      });

    }

    if (dragStartListener != null) {
      icon.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
          if (MotionEventCompat.getActionMasked(motionEvent) ==
              MotionEvent.ACTION_DOWN) {
            dragStartListener.startDrag(MediaViewHolder.this);
          }
          return false;
        }
      });
    }
  }

  public void bindMediaItem(@NonNull final MediaItem mediaItem, boolean activated,
                            boolean enabled) {
    this.mediaItem = mediaItem;

    title.setText(mediaItem.title);
    CharSequence subtext = TextUtils.concat(mediaItem.artist, " - ",
        MathUtil.getSongDuration(mediaItem.getDuration()));
    subText.setText(subtext);

    Glide.with(albumArt.getContext()).load(mediaItem.thumbnailUri).crossFade()
        .fallback(R.drawable.album_art_empty).into(albumArt);

    if (dragStartListener == null) {
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
    } else {
      itemView.setOnClickListener(
          dragStartListener.canClick(mediaItem) ? new View.OnClickListener() {


            @Override
            public void onClick(View view) {
              dragStartListener.onMediaItemClicked(mediaItem);
            }
          } : null);
    }

    bindMediaItemPartial(activated, enabled);
  }

  public void bindMediaItemPartial(boolean activated, boolean enabled) {
    itemView.setActivated(activated);
    itemView.setEnabled(enabled);

    if (dragStartListener != null) {
      if (dragStartListener.canDrag(mediaItem, activated, enabled)) {
        icon.setImageResource(R.drawable.ic_drag_handle);
        icon.setVisibility(View.VISIBLE);
      } else {
        icon.setVisibility(View.INVISIBLE);
      }
      menu.setVisibility(View.GONE);
    }
  }

}
