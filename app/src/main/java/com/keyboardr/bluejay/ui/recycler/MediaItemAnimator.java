package com.keyboardr.bluejay.ui.recycler;

import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

/**
 * Animates MediaItems in a RecyclerView
 */

public class MediaItemAnimator extends DefaultItemAnimator {

  @Override
  public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
    return true;
  }
}
