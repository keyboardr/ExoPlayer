package com.keyboardr.bluejay.ui.playlist;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.TooltipHelper;

public class PlaylistControlsUpdater extends PlayerControlsUpdater<PlaylistServiceClient> {

  private PopupWindow popupWindow;

  public PlaylistControlsUpdater(@NonNull View view, @NonNull PlaylistServiceClient player,
                                 @NonNull LoaderManager loaderManager) {
    super(view, player, loaderManager);
  }

  @Override
  protected void attachPlayer() {
    super.attachPlayer();
    playPause.setImageResource(R.drawable.asl_none_single_continuous);
    playPause.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        if (player.isPlaying()) {
          showFadeOut();
          return true;
        } else {
          return false;
        }
      }
    });
  }

  private void showFadeOut() {
    popupWindow = new PopupWindow(playPause.getContext());
    @SuppressLint("InflateParams") final
    View content = LayoutInflater.from(playPause.getContext()).inflate(R.layout.popup_fadeout,
        null);
    final View cancelButton = content.findViewById(R.id.cancel);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        player.setVolume(1);
        popupWindow.dismiss();
      }
    });
    TooltipHelper.addTooltip(cancelButton, true);
    cancelButton.setContentDescription(playPause.getContext()
        .getText(R.string.description_cancel_fadeout));
    SeekBar seekBar = (SeekBar) content.findViewById(R.id.fader);
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      private int lastProgress = 100;

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > lastProgress) {
          seekBar.setProgress(lastProgress);
        } else {
          player.setVolume((float) progress / (float) seekBar.getMax());
          lastProgress = progress;
        }
        if (progress < 95) {
          cancelButton.setContentDescription(cancelButton.getContext()
              .getText(R.string.description_slide_to_fadeout));
          TooltipHelper.showTooltipOnClick(cancelButton, true);
          cancelButton.setActivated(true);
          cancelButton.setPressed(true);
        }
        if (progress == 0) {
          seekBar.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK,
              HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
          player.pause();
          popupWindow.dismiss();
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        if (cancelButton.isActivated()) {
          cancelButton.setPressed(true);
        }
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        cancelButton.setPressed(false);
      }
    });

    popupWindow.setContentView(content);
    popupWindow.setOverlapAnchor(true);
    float density = content.getResources().getDisplayMetrics().density;
    popupWindow.setElevation(24f * density);
    popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
      @Override
      public void onDismiss() {
        popupWindow = null;
      }
    });
    popupWindow.showAsDropDown(playPause, (int) (-16 * density), (int) (-12 * density));
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
  protected void onItemChanged(@Nullable MediaItem mediaItem) {
    if (popupWindow != null) {
      popupWindow.dismiss();
    }
  }

  @Override
  protected void onPlayClicked() {
    if (player.getCurrentMediaItem() != null) {
      player.togglePlayPause();
    }
  }
}
