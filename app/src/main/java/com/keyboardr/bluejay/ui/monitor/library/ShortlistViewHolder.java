package com.keyboardr.bluejay.ui.monitor.library;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.keyboardr.bluejay.model.Shortlist;

/**
 * Manages view state for checkable Shortlist selectors while delegating business logic to
 * subclasses
 */

public abstract class ShortlistViewHolder extends RecyclerView.ViewHolder implements View
    .OnClickListener {
  protected Shortlist shortlist;
  private CheckedTextView checkableView;

  public ShortlistViewHolder(ViewGroup parent) {
    super(LayoutInflater.from(parent.getContext()).inflate(
        android.R.layout.simple_list_item_multiple_choice, parent, false));
    checkableView = (CheckedTextView) itemView.findViewById(android.R.id.text1);
    checkableView.setOnClickListener(this);
  }

  public void bindItem(@NonNull Shortlist shortlist) {
    this.shortlist = shortlist;
    checkableView.setText(shortlist.getName());
    updateState();
  }

  private void updateState() {
    checkableView.setChecked(getCheckedState() != null);
    if (getCheckedState() == Boolean.FALSE) {
      checkableView.setPaintFlags(checkableView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    } else {
      checkableView.setPaintFlags(checkableView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
    }
  }

  @Override
  public void onClick(View view) {
    toggleState();
    updateState();
  }

  protected abstract void toggleState();

  @Nullable
  protected abstract Boolean getCheckedState();
}
