package com.keyboardr.bluejay.ui.shortlists;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.keyboardr.bluejay.provider.ShortlistManager;

/**
 * Simple adapter for showing all shortlists in the ShortlistManager
 */
public abstract class ShortlistAdapter<T extends ShortlistViewHolder>
    extends RecyclerView.Adapter<T> {

  private static final String TAG = "ShortlistAdapter";

  private final ShortlistManager shortlistManager;

  public ShortlistAdapter(@NonNull ShortlistManager shortlistManager) {
    this.shortlistManager = shortlistManager;
    setHasStableIds(true);
  }

  @Override
  public void onBindViewHolder(T holder, int position) {
    holder.bindItem(shortlistManager.getShortlists().get(position));
  }

  @Override
  public int getItemCount() {
    if (shortlistManager.isReady()) {
      return shortlistManager.getShortlists().size();
    } else {
      Log.w(TAG, "getItemCount: ShortlistManager not ready");
      return 0;
    }
  }

  @Override
  public long getItemId(int position) {
    return shortlistManager.getShortlists().get(position).getId();
  }
}
