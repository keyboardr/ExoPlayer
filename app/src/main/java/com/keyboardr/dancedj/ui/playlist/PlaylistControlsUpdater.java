package com.keyboardr.dancedj.ui.playlist;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.View;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.service.PlaylistServiceClient;
import com.keyboardr.dancedj.ui.PlayerControlsUpdater;

public class PlaylistControlsUpdater extends PlayerControlsUpdater<PlaylistServiceClient> {

    public PlaylistControlsUpdater(@NonNull View view, @NonNull PlaylistServiceClient player,
                                   @NonNull LoaderManager loaderManager) {
        super(view, player, loaderManager);
    }

    @Override
    protected void updatePlayPauseButton() {
        playPause.setImageResource(R.drawable.ic_play_arrow);
        playPause.setEnabled(player.isPaused() || player.isStopped());
    }

    @Override
    protected void onPlayClicked() {
        if (player.getCurrentMediaItem() != null) {
            player.togglePlayPause();
        }
    }
}
