package com.keyboardr.bluejay.ui.monitor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.widget.SeekBar;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.player.MonitorPlayer;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;

public class MonitorControlsUpdater extends PlayerControlsUpdater<MonitorPlayer> {

  public MonitorControlsUpdater(@NonNull View view, @NonNull MonitorPlayer player,
                                @NonNull LoaderManager loaderManager,
                                @Nullable OnAlbumArtListener albumArtListener) {
    super(view, player, loaderManager, albumArtListener);
  }

  @Override
  protected void attachPlayer() {
    super.attachPlayer();
    if (seekBar instanceof SeekBar) {
      ((SeekBar) seekBar).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
          if (fromUser && player.getDuration() > 0) {
            player.seekTo((int) (((float) progress) * ((float) player.getDuration())
                                     / (float) seekBar.getMax()));
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
      });
    }
    playPause.setImageResource(R.drawable.asl_play_pause);
  }

  @Override
  protected void updatePlayPauseButton() {
    playPause.setActivated(player.isPlaying());
  }

  @Override
  protected void onPlayClicked() {
    if (player.getCurrentMediaItem() != null) {
      player.togglePlayPause();
    }
  }
}
