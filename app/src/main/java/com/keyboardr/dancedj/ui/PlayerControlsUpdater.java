package com.keyboardr.dancedj.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.AbsPlayer;
import com.keyboardr.dancedj.player.Player;
import com.keyboardr.dancedj.ui.monitor.MonitorControlsFragment;
import com.keyboardr.dancedj.util.MathUtil;

/**
 * Responsible for updating the UI of a controller
 */
public abstract class PlayerControlsUpdater<P extends Player> implements AbsPlayer.PlaybackListener {

    private static final String ARG_MEDIA_ITEM = "mediaItem";

    protected final ImageView playPause;
    protected final ProgressBar seekBar;
    @NonNull
    private final View view;
    private final ImageView albumArt;
    private final TextView title;
    private final TextView artist;

    @NonNull
    private final LoaderManager loaderManager;

    private MediaItem lastMediaItem;

    protected final P player;

    private Handler seekHandler = new Handler();

    private Runnable seekRunnable;

    private Bitmap albumArtData;

    private LoaderManager.LoaderCallbacks<Bitmap> albumArtCalbacks = new LoaderManager.LoaderCallbacks<Bitmap>() {
        @Override
        public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
            return new MonitorControlsFragment.AlbumArtLoader(view.getContext(), (MediaItem) args.getParcelable(ARG_MEDIA_ITEM));
        }

        @Override
        public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
            albumArtData = data;
            if (albumArt != null) {
                albumArt.setImageBitmap(albumArtData);
            }
        }

        @Override
        public void onLoaderReset(Loader<Bitmap> loader) {
            albumArtData = null;
        }
    };

    public PlayerControlsUpdater(@NonNull View view, @NonNull P player, @NonNull LoaderManager loaderManager) {
        this.view = view;
        this.player = player;
        this.loaderManager = loaderManager;

        title = ((TextView) view.findViewById(R.id.controls_title));
        artist = ((TextView) view.findViewById(R.id.controls_artist));
        seekBar = (ProgressBar) view.findViewById(R.id.controls_seek);
        playPause = ((ImageView) view.findViewById(R.id.controls_play_pause));
        albumArt = (ImageView) view.findViewById(R.id.controls_album_art);

        attachPlayer();
    }

    public void onMetaData() {
        MediaItem mediaItem = player.getCurrentMediaItem();
        boolean itemChanged = lastMediaItem != mediaItem;
        lastMediaItem = mediaItem;
        title.setText(mediaItem == null ? "" : mediaItem.title);
        artist.setText(mediaItem == null ? "" : mediaItem.artist);

        updateVisibility(mediaItem);

        albumArt.setImageBitmap(albumArtData);

        Bundle loaderArgs = new Bundle();
        loaderArgs.putParcelable(ARG_MEDIA_ITEM, mediaItem);

        if (!itemChanged) {
            loaderManager.initLoader(0, loaderArgs, albumArtCalbacks);
        } else {
            loaderManager.restartLoader(0, loaderArgs, albumArtCalbacks);
        }
    }

    private void updateVisibility(@Nullable MediaItem item) {
        playPause.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
        seekBar.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
        title.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
        artist.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
        albumArt.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
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
        if (player.isPlaying()) {
            seekRunnable = new Runnable() {
                @Override
                public void run() {
                    onSeekComplete(player);
                    seekHandler.postDelayed(this, player.getDuration() / seekBar.getMax());
                }
            };
            seekHandler.post(seekRunnable);
        } else {
            seekHandler.removeCallbacks(seekRunnable);
        }
    }

    protected abstract void updatePlayPauseButton();

    protected abstract void onPlayClicked();
}
