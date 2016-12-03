package com.keyboardr.bluejay.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class FragmentUtils {

  /**
   * @param fragment    The Fragment whose parent is to be found
   * @param parentClass The interface that the parent should implement
   * @return The parent of fragment that implements parentClass,
   * null, if no such parent can be found
   */
  @Nullable
  @CheckResult(suggest = "#checkParent(Fragment, Class)")
  public static <T> T getParent(@NonNull Fragment fragment, @NonNull Class<T> parentClass) {
    Fragment parentFragment = fragment.getParentFragment();
    if (parentClass.isInstance(parentFragment)) {
      //Checked by runtime methods
      //noinspection unchecked
      return (T) parentFragment;
    } else if (parentClass.isInstance(fragment.getActivity())) {
      //Checked by runtime methods
      //noinspection unchecked
      return (T) fragment.getActivity();
    }
    return null;
  }

  @NonNull
  public static <T> T getParentChecked(@NonNull Fragment fragment, @NonNull Class<T> parentClass) {
    return getNonNullParent(fragment, parentClass);
  }

  public static void checkParent(@NonNull Fragment fragment, @NonNull Class parentClass) {
    getNonNullParent(fragment, parentClass);
  }

  @NonNull
  private static <T> T getNonNullParent(@NonNull Fragment fragment, @NonNull Class<T> parentClass) {
    T parent = getParent(fragment, parentClass);
    if (parent == null) {
      Object actualParent = fragment.getParentFragment();
      if (actualParent == null) {
        actualParent = fragment.getActivity();
      }
      throw new IllegalStateException("Parent must implement " + parentClass.getSimpleName()
          + ". Instead found " + actualParent.getClass().getCanonicalName());
    } else {
      return parent;
    }
  }
}