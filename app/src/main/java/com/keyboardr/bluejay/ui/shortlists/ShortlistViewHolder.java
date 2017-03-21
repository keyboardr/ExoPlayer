package com.keyboardr.bluejay.ui.shortlists;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.keyboardr.bluejay.model.Shortlist;

/**
 * Manages view state for checkable Shortlist selectors while delegating business logic to
 * subclasses
 */

public abstract class ShortlistViewHolder extends RecyclerView.ViewHolder implements View
    .OnClickListener {
  protected Shortlist shortlist;
  protected TextView textView;

  public ShortlistViewHolder(ViewGroup parent, @LayoutRes int layout) {
    super(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    textView = (TextView) itemView.findViewById(android.R.id.text1);
    itemView.setOnClickListener(this);
  }

  public void bindItem(@NonNull Shortlist shortlist) {
    this.shortlist = shortlist;
    textView.setText(shortlist.getName());
  }

}
