package com.keyboardr.bluejay.ui.monitor.library;

import android.support.annotation.NonNull;
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
    checkableView.setChecked(isChecked());
  }

  @Override
  public void onClick(View view) {
    checkableView.toggle();
    boolean checked = checkableView.isChecked();
    setChecked(checked);
  }

  protected abstract void setChecked(boolean checked);

  protected abstract boolean isChecked();
}
