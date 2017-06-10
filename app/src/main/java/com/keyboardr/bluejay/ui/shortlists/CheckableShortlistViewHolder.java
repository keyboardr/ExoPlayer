package com.keyboardr.bluejay.ui.shortlists;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.Shortlist;

/**
 * A version of ShortlistViewHolder that deals with checkable shortlists. May be tri-state or
 * dual-state.
 */

public abstract class CheckableShortlistViewHolder extends ShortlistViewHolder {
  private CheckBox checkableView;

  public CheckableShortlistViewHolder(ViewGroup parent) {
    super(parent, R.layout.item_shortlist);
    checkableView = ((CheckBox) textView);
  }

  @Override
  public void bindItem(@NonNull Shortlist shortlist) {
    super.bindItem(shortlist);
    updateState();
  }

  @Override
  public void onClick(View view) {
    toggleState();
    updateState();
  }

  private void updateState() {
    checkableView.setChecked(getCheckedState() != null);
    if (getCheckedState() == Boolean.FALSE) {
      checkableView.setPaintFlags(checkableView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      checkableView.setSelected(false);
    } else {
      checkableView.setPaintFlags(checkableView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
      checkableView.setSelected(true);
    }
  }

  protected abstract void toggleState();

  @Nullable
  protected abstract Boolean getCheckedState();
}
