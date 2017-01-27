package com.keyboardr.bluejay.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.AbsPlayer;
import com.keyboardr.bluejay.player.Player;
import com.keyboardr.bluejay.ui.monitor.MonitorControlsFragment;
import com.keyboardr.bluejay.util.MathUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for updating the UI of a controller
 */
public abstract class PlayerControlsUpdater<P extends Player> implements AbsPlayer
    .PlaybackListener {

  public static final AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new
      AccelerateDecelerateInterpolator();
  public static final AnticipateInterpolator ANTICIPATE_INTERPOLATOR = new AnticipateInterpolator();
  public static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator();

  public interface OnAlbumArtListener {
    void onAlbumArtReset();

    void onAlbumArtReady(@Nullable Icon albumArt);
  }

  private static final String ARG_MEDIA_ITEM = "mediaItem";

  protected final ImageView playPause;
  protected final ProgressBar seekBar;
  @NonNull
  private final View view;
  private final ImageView albumArt;
  private final ViewGroup albumArtContainer;
  private final ImageView albumArtOverlay;
  private final TextView title;
  private final TextView artist;
  private final TextView position;
  private final TextView duration;
  private final View backgroundOverlay;

  @Nullable
  private final OnAlbumArtListener albumArtListener;

  @NonNull
  private final LoaderManager loaderManager;

  @NonNull
  protected final P player;

  private final Handler seekHandler = new Handler();

  @NonNull
  private final Runnable seekRunnable = new Runnable() {
    @Override
    public void run() {
      onSeekComplete(player);
      seekHandler.postDelayed(this, 1000 - (player.getCurrentPosition() % 1000));
    }
  };

  private Icon albumArtData;

  private MediaItem lastMediaItem;

  private LoaderManager.LoaderCallbacks<Pair<Icon, Palette>> albumArtCalbacks = new LoaderManager
      .LoaderCallbacks<Pair<Icon, Palette>>() {

    @Override
    public Loader<Pair<Icon, Palette>> onCreateLoader(int id, Bundle args) {
      return new MonitorControlsFragment.AlbumArtLoader(view.getContext(),
          (MediaItem) args.getParcelable(ARG_MEDIA_ITEM));
    }

    @Override
    public void onLoadFinished(Loader<Pair<Icon, Palette>> loader,
                               @Nullable Pair<Icon, Palette> data) {
      if (Objects.equals(albumArtData, data == null ? null : data.first)) {
        return;
      }
      boolean entering = albumArtData == null;
      boolean exiting = data == null || data.first == null;

      albumArtData = data == null ? null : data.first;
      if (albumArt != null) {
        final int fullDuration = albumArtContainer.getContext().getResources()
            .getInteger(android.R.integer.config_mediumAnimTime);
        if (exiting) {
          backgroundOverlay.setBackground(view.getBackground());
          backgroundOverlay.setVisibility(View.VISIBLE);
          view.setBackground(null);
          int startX = albumArtContainer.getLeft() + albumArtContainer.getWidth() / 2;
          int startY = albumArtContainer.getTop() + albumArtContainer.getHeight() / 2;
          float startRadius = (float) Math.hypot(view.getWidth() - startX,
              view.getHeight() - startY);
          Animator backgroundReveal = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
              startX, startY, startRadius, 0);
          backgroundReveal.setDuration(fullDuration / 2);
          backgroundReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              backgroundOverlay.setVisibility(View.GONE);
            }
          });
          backgroundReveal.start();

          albumArtContainer.animate().scaleX(0).scaleY(0)
              .setDuration(fullDuration).setInterpolator(ANTICIPATE_INTERPOLATOR)
              .withEndAction(new Runnable() {
                @Override
                public void run() {
                  albumArt.setImageIcon(albumArtData);
                  albumArtContainer.setVisibility(View.INVISIBLE);
                  albumArtContainer.setScaleX(1);
                  albumArtContainer.setScaleY(1);
                }
              });
        } else {
          final int color = data.second.getDarkMutedColor(
              backgroundOverlay.getContext().getColor(R.color.colorPrimaryDark));
          backgroundOverlay.setBackgroundColor(color);
          backgroundOverlay.setVisibility(View.VISIBLE);
          int startX = albumArtContainer.getLeft() + albumArtContainer.getWidth() / 2;
          int startY = albumArtContainer.getTop() + albumArtContainer.getHeight() / 2;
          float endRadius = (float) Math.hypot(view.getWidth() - startX,
              view.getHeight() - startY);
          Animator backgroundReveal = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
              startX, startY, 0, endRadius);
          backgroundReveal.setDuration(fullDuration / 2);
          backgroundReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              ((ViewGroup) backgroundOverlay.getParent()).setBackgroundColor(color);
              backgroundOverlay.setVisibility(View.GONE);
            }
          });
          backgroundReveal.start();


          if (entering) {
            albumArt.setImageIcon(albumArtData);
            albumArtContainer.setScaleX(0);
            albumArtContainer.setScaleY(0);
            albumArtContainer.setVisibility(View.VISIBLE);
            albumArtContainer.animate().scaleX(1).scaleY(1)
                .setDuration(fullDuration).setInterpolator(OVERSHOOT_INTERPOLATOR);
          } else {
            ViewPropertyAnimator animation = albumArtContainer.animate().scaleX(1.2f).scaleY(1.2f)
                .translationZ(4f)
                .setDuration(fullDuration / 2).setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR)
                .withEndAction(new Runnable() {
                  @Override
                  public void run() {
                    albumArtContainer.animate().scaleX(1f).scaleY(1).translationZ(0)
                        .setDuration(fullDuration / 2)
                        .setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
                  }
                });
            albumArtOverlay.setImageIcon(albumArtData);
            int centerX = albumArtOverlay.getWidth() /
                2;
            int centerY = albumArtOverlay.getHeight() / 2;
            Animator albumArtReveal = ViewAnimationUtils.createCircularReveal(albumArtOverlay,
                centerX, centerY, 0,
                (float) Math.hypot(centerX, centerY));
            albumArtReveal.setDuration(animation.getDuration() * 2);
            albumArtOverlay.setVisibility(View.VISIBLE);
            albumArtReveal.addListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                albumArt.setImageIcon(albumArtData);
                albumArtOverlay.setVisibility(View.INVISIBLE);
              }
            });
            albumArtReveal.start();
          }
        }
      }
      if (albumArtListener != null) {
        albumArtListener.onAlbumArtReady(albumArtData);
      }
    }

    @Override
    public void onLoaderReset(Loader<Pair<Icon, Palette>> loader) {
      albumArtData = null;
      if (albumArtListener != null) {
        albumArtListener.onAlbumArtReset();
      }
    }
  };

  public PlayerControlsUpdater(@NonNull View view, @NonNull P player,
                               @NonNull LoaderManager loaderManager,
                               @Nullable OnAlbumArtListener albumArtListener) {
    this.view = view;
    this.player = player;
    this.loaderManager = loaderManager;
    this.albumArtListener = albumArtListener;

    title = (TextView) view.findViewById(R.id.controls_title);
    artist = (TextView) view.findViewById(R.id.controls_artist);
    position = (TextView) view.findViewById(R.id.controls_position);
    duration = (TextView) view.findViewById(R.id.controls_duration);
    seekBar = (ProgressBar) view.findViewById(R.id.controls_seek);
    playPause = (ImageView) view.findViewById(R.id.controls_play_pause);
    albumArt = (ImageView) view.findViewById(R.id.controls_album_art);
    albumArtContainer = (ViewGroup) view.findViewById(R.id.controls_album_art_container);
    albumArtOverlay = (ImageView) view.findViewById(R.id.controls_album_art_overlay);
    backgroundOverlay = view.findViewById(R.id.controls_background_overlay);

    attachPlayer();
  }

  public void onMetaData() {
    MediaItem mediaItem = player.getCurrentMediaItem();
    boolean itemChanged = !Objects.equals(lastMediaItem, mediaItem);
    lastMediaItem = mediaItem;
    title.setText(
        mediaItem == null ? view.getContext().getText(R.string.no_media_playing) : mediaItem.title);
    artist.setText(mediaItem == null ? "" : mediaItem.artist);
    seekBar.setMax(mediaItem == null ? 0 : (int) Math.ceil(mediaItem.getDuration() / 1000));

    updateVisibility(mediaItem != null);

    Bundle loaderArgs = new Bundle();
    loaderArgs.putParcelable(ARG_MEDIA_ITEM, mediaItem);

    if (itemChanged) {
      loaderManager.restartLoader(0, loaderArgs, albumArtCalbacks);
    }
  }

  private void updateVisibility(boolean visible) {
    playPause.setEnabled(visible);
    seekBar.setEnabled(visible);
    title.setEnabled(visible);
    artist.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    position.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    duration.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
  }

  @Override
  public void onSeekComplete(Player player) {
    float currentPosition = player.getCurrentPosition();
    float duration = player.getDuration();
    float max = seekBar.getMax();
    int progress = duration == 0 ? 0 : MathUtil.clamp(
        (int) ((currentPosition / duration) * max + .5f),
        0, seekBar.getMax());
    seekBar.setProgress(progress);
    this.duration.setText(MathUtil.getSongDuration((long) duration, false));
    position.setText(MathUtil.getSongDuration((long) currentPosition,
        TimeUnit.MILLISECONDS.toHours((long) duration) >= 1));
  }

  @Override
  public void onPlayStateChanged(Player player) {
    onMetaData();
    updatePlayState();
  }

  protected void attachPlayer() {
    player.setPlaybackListener(this);

    onMetaData();
    updatePlayState();

    playPause.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onPlayClicked();
      }
    });
  }

  public void detach() {
    player.setPlaybackListener(null);
    seekHandler.removeCallbacks(seekRunnable);
  }

  public void updatePlayState() {
    updatePlayPauseButton();
    seekHandler.removeCallbacks(seekRunnable);
    if (player.isPlaying()) {
      seekHandler.post(seekRunnable);
    }
  }

  protected abstract void updatePlayPauseButton();

  protected abstract void onPlayClicked();
}
