package com.keyboardr.bluejay.model;

import android.os.Bundle;

/**
 * Info about a set that does not change from the time it is started until it ends
 */

public class SetMetadata {
  private static final String ARG_NAME = "name";
  private static final String ARG_IS_SOUND_CHECK = "isSoundCheck";

  public final String name;
  public final boolean isSoundCheck;

  public SetMetadata(String name, boolean isSoundCheck) {
    this.name = name;
    this.isSoundCheck = isSoundCheck;
  }

  public SetMetadata(Bundle bundle) {
    this.name = bundle.getString(ARG_NAME);
    this.isSoundCheck = bundle.getBoolean(ARG_IS_SOUND_CHECK);
  }

  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putString(ARG_NAME, name);
    bundle.putBoolean(ARG_IS_SOUND_CHECK, isSoundCheck);
    return bundle;
  }
}
