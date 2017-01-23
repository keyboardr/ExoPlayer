package com.keyboardr.bluejay.ui;

import android.graphics.drawable.Icon;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Methods related to the bottom nav
 */

public interface BottomNavHolder {
  void setMonitorAlbumArt(@Nullable Icon albumArt);
  void setPlaylistAlbumArt(@Nullable Icon albumArt);
  @Nullable View getPlaylistTabView();
}
