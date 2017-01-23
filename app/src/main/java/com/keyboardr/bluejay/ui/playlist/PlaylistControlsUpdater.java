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
  protected void attachPlayer() {
    super.attachPlayer();
    playPause.setImageResource(R.drawable.asl_none_single_continuous);
  }

  @Override
  protected void updatePlayPauseButton() {
    boolean activated = player.isPlaying() || player.willContinuePlayingOnDone();
    if (activated) {
      playPause.setActivated(true);
      playPause.setImageState(
          new int[]{(player.willContinuePlayingOnDone() ? 1 : -1) * R.attr.state_continuous},
          true);
    } else {
      playPause.setActivated(false);
      playPause.setImageState(
          new int[]{R.attr.state_continuous},
          true);
    }
  }

  @Override
  protected void onPlayClicked() {
    if (player.getCurrentMediaItem() != null) {
      player.togglePlayPause();
    }
  }
}
