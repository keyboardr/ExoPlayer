package com.keyboardr.bluejay.util;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

/**
 * Shows tooltips on long press
 */

public class TooltipHelper {

  private TooltipHelper() {
  }

  public static void addTooltip(@NonNull View view) {
    addTooltip(view, false);
  }

  public static void addTooltip(@NonNull View view, boolean below) {
    view.setOnLongClickListener(new TooltipLongPressListener(below));
  }

  private static class TooltipLongPressListener implements View.OnLongClickListener {
    final boolean below;

    private TooltipLongPressListener(boolean below) {
      this.below = below;
    }

    @Override
    public boolean onLongClick(View view) {
      CharSequence description = view.getContentDescription();

      final int[] screenPos = new int[2];
      final Rect displayFrame = new Rect();
      view.getLocationOnScreen(screenPos);
      view.getWindowVisibleDisplayFrame(displayFrame);

      final int width = view.getWidth();
      final int height = view.getHeight();
      final int midy = screenPos[1] + height / 2;
      int referenceX = screenPos[0] + width / 2;
      if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
        final int screenWidth = view.getResources().getDisplayMetrics().widthPixels;
        referenceX = screenWidth - referenceX; // mirror
      }
      Toast cheatSheet = Toast.makeText(view.getContext(), description, Toast.LENGTH_SHORT);
      if (midy < displayFrame.height()) {
        final int screenWidth = view.getResources().getDisplayMetrics().widthPixels;
        // Show along the top; follow action buttons
        int xOffset = screenWidth / 2 - referenceX;
        if (below) {
          cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, xOffset,
              screenPos[1] + height - displayFrame.top);
        } else {
          cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, xOffset,
              displayFrame.bottom - screenPos[1]);
        }
      } else {
        // Show along the bottom center
        cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
      }
      cheatSheet.show();
      return true;
    }
  }
}
