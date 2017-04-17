package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * A SeekBar that only allows sliding from thumb
 */

public class Slider extends android.support.v7.widget.AppCompatSeekBar {
  private Drawable thumb;

  public Slider(Context context) {
    super(context);
  }

  public Slider(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setThumb(Drawable thumb) {
    super.setThumb(thumb);
    this.thumb = thumb;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
      if (event.getX() >= thumb.getBounds().left - touchSlop
          && event.getX() <= thumb.getBounds().right + touchSlop) {
        super.onTouchEvent(event);
      } else {
        return false;
      }
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      return isPressed();
    } else {
      super.onTouchEvent(event);
    }

    return true;
  }
}
