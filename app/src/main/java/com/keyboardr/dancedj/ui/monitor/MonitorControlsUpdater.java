package com.keyboardr.dancedj.ui.monitor;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.View;
import android.widget.SeekBar;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.player.MonitorPlayer;
import com.keyboardr.dancedj.ui.PlayerControlsUpdater;

public class MonitorControlsUpdater extends PlayerControlsUpdater<MonitorPlayer> {

    public MonitorControlsUpdater(@NonNull View view, @NonNull MonitorPlayer player,
                                  @NonNull LoaderManager loaderManager) {
        super(view, player, loaderManager);
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
    }

    @Override
    protected void updatePlayPauseButton() {
        playPause.setImageResource(player.isPlaying() ?
                R.drawable.ic_pause : R.drawable.ic_play_arrow);
    }

    @Override
    protected void onPlayClicked() {
        if (player.getCurrentMediaItem() != null) {
            player.togglePlayPause();
        }
    }
}
