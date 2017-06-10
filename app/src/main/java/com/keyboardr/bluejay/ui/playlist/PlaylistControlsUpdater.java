package com.keyboardr.bluejay.ui.playlist;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.TooltipHelper;

public class PlaylistControlsUpdater extends PlayerControlsUpdater<PlaylistServiceClient> {

  @NonNull private final FragmentManager childFragmentManager;
  private PopupWindow fadeOutWindow;
  private PopupMenu menu;

  private final PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu
      .OnMenuItemClickListener() {

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
      switch (menuItem.getItemId()) {
        case R.id.end_set:
          new PlaylistControlsFragment.EndSetDialogFragment().show(childFragmentManager, null);
          return true;
        case R.id.fade_out:
          showFadeOut();
          return true;
      }
      return false;
    }
  };

  public PlaylistControlsUpdater(@NonNull View view, @NonNull PlaylistServiceClient player,
                                 @NonNull LoaderManager loaderManager,
                                 @NonNull FragmentManager childFragmentManager) {
    super(view, player, loaderManager);
    this.childFragmentManager = childFragmentManager;

    view.findViewById(R.id.controls_menu).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.frag_set);
        menu.setOnMenuItemClickListener(menuListener);
        menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
          @Override
          public void onDismiss(PopupMenu popupMenu) {
            menu = null;
          }
        });
        populateMenu(menu.getMenu());
        menu.show();
      }
    });
  }

  private void populateMenu(Menu menu) {
    menu.findItem(R.id.fade_out).setEnabled(player.isPlaying());
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
    fadeOutWindow = new PopupWindow(playPause.getContext());
    @SuppressLint("InflateParams") final
    View content = LayoutInflater.from(playPause.getContext()).inflate(R.layout.popup_fadeout,
        null);
    final View cancelButton = content.findViewById(R.id.cancel);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        player.setVolume(1);
        fadeOutWindow.dismiss();
      }
    });
    TooltipHelper.addTooltip(cancelButton, true);
    cancelButton.setContentDescription(playPause.getContext()
        .getText(R.string.description_cancel_fadeout));
    SeekBar seekBar = content.findViewById(R.id.fader);
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
          fadeOutWindow.dismiss();
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

    fadeOutWindow.setContentView(content);
    fadeOutWindow.setOverlapAnchor(true);
    float density = content.getResources().getDisplayMetrics().density;
    fadeOutWindow.setElevation(24f * density);
    fadeOutWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
      @Override
      public void onDismiss() {
        fadeOutWindow = null;
      }
    });
    fadeOutWindow.showAsDropDown(playPause, (int) (-12 * density), (int) (-32 * density));
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
    if (menu != null) {
      populateMenu(menu.getMenu());
    }
  }

  @Override
  protected void onItemChanged(@Nullable MediaItem mediaItem) {
    if (fadeOutWindow != null) {
      fadeOutWindow.dismiss();
    }
  }

  @Override
  protected void onPlayClicked() {
    if (player.getCurrentMediaItem() != null) {
      player.togglePlayPause();
    }
  }
}
