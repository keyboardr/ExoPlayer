package com.keyboardr.bluejay.ui.playlist;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.view.View;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;

public class PlaylistControlsUpdater extends PlayerControlsUpdater<PlaylistServiceClient> {

  public PlaylistControlsUpdater(@NonNull View view, @NonNull PlaylistServiceClient player,
                                 @NonNull LoaderManager loaderManager,
                                 @NonNull OnAlbumArtListener albumArtListener) {
    super(view, player, loaderManager, albumArtListener);
  }

  @Override
  protected void updatePlayPauseButton() {
    if (player.isPaused() || player.isStopped()) {
      playPause.setImageResource(R.drawable.ic_play_arrow);
    } else {
      playPause.setImageResource(player.willContinuePlayingOnDone()
          ? R.drawable.ic_play_circle_filled : R.drawable.ic_play_circle_outline);
    }
  }

  @Override
  protected void onPlayClicked() {
    if (player.getCurrentMediaItem() != null) {
      player.togglePlayPause();
    }
  }
}
