package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

/**
 * A SeekBar that only allows sliding from thumb
 */

public class Slider extends VerticalSeekBar {
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
      if (event.getY() >= thumb.getBounds().top - touchSlop
          && event.getY() <= thumb.getBounds().bottom + touchSlop) {
        super.onTouchEvent(event);
      } else {
        return false;
      }
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      //noinspection SimplifiableIfStatement
      if (isPressed()) {
        return super.onTouchEvent(event);
      }
      return false;
    } else {
      super.onTouchEvent(event);
    }

    return true;
  }
}
