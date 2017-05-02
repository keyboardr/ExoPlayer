package com.keyboardr.bluejay.model;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Info about a set that does not change from the time it is started until it ends
 */

public class SetMetadata {
  private static final String ARG_NAME = "name";
  private static final String ARG_HAS_SETLIST_ID = "hasSetlistId";
  private static final String ARG_SETLIST_ID = "setlistId";
  private static final String ARG_IS_SOUND_CHECK = "isSoundCheck";

  public final String name;
  @Nullable public final Long setlistId;
  public final boolean isSoundCheck;

  public static SetMetadata withSetlistId(@NonNull SetMetadata setMetadata, long setlistId) {
    if (setMetadata.setlistId != null && setMetadata.setlistId == setlistId) {
      return setMetadata;
    }
    return new SetMetadata(setMetadata.name, setlistId, setMetadata.isSoundCheck);
  }

  public SetMetadata(String name, @Nullable Long setlistId, boolean isSoundCheck) {
    this.name = name;
    this.setlistId = setlistId;
    this.isSoundCheck = isSoundCheck;
  }

  public SetMetadata(Bundle bundle) {
    this.name = bundle.getString(ARG_NAME);
    this.setlistId = bundle.getBoolean(ARG_HAS_SETLIST_ID, false) ? bundle.getLong
        (ARG_SETLIST_ID) : null;
    this.isSoundCheck = bundle.getBoolean(ARG_IS_SOUND_CHECK);
  }

  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putString(ARG_NAME, name);
    bundle.putBoolean(ARG_HAS_SETLIST_ID, setlistId != null);
    if (setlistId != null) {
      bundle.putLong(ARG_SETLIST_ID, setlistId);
    }
    bundle.putBoolean(ARG_IS_SOUND_CHECK, isSoundCheck);
    return bundle;
  }
}
