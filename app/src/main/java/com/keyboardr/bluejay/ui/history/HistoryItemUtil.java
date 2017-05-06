package com.keyboardr.bluejay.ui.history;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.provider.SetlistItemContract;

/**
 * Created by Keyboardr on 5/5/2017.
 */

public class HistoryItemUtil {

  private HistoryItemUtil() {
  }

  public static boolean onMenuItemClick(@NonNull Fragment fragment,
                                        @NonNull String setlistName, long setlistId,
                                        @IdRes int itemId) {
    switch (itemId) {
      case R.id.rename:
        SetlistRenameDialogFragment.newInstance(setlistName, setlistId)
            .show(fragment.getChildFragmentManager(), null);
        return true;
      case R.id.share:
        SetlistItemContract.Utils.shareSetlist(fragment.getActivity(), setlistName, setlistId);
        return true;
      case R.id.delete:
        DeleteSetlistDialogFragment.newInstance(setlistName, setlistId)
            .show(fragment.getChildFragmentManager(), null);
        return true;
    }
    return false;
  }
}
