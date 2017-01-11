package com.keyboardr.bluejay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistMediaService;
import com.keyboardr.bluejay.ui.NoSetFragment;
import com.keyboardr.bluejay.ui.monitor.MonitorControlsFragment;
import com.keyboardr.bluejay.ui.monitor.library.LibraryFragment;
import com.keyboardr.bluejay.ui.playlist.SetFragment;

public class PlaybackActivity extends AppCompatActivity implements LibraryFragment.Holder,
    NoSetFragment.Holder, SetFragment.Holder {

  private static final String STATE_SHOW_PLAYLIST = "showPlaylist";
  @Nullable
  private ViewSwitcher monitorPlaylistSwitcher;

  private static final int INDEX_MONITOR = 0;
  private static final int INDEX_PLAYLIST = 1;

  private static final String TAG = "PlaybackActivity";

  private MediaBrowserCompat mediaBrowser;

  private MediaBrowserCompat.ConnectionCallback playlistServiceConn = new MediaBrowserCompat
      .ConnectionCallback() {
    @Override
    public void onConnected() {
      Fragment frag = getSupportFragmentManager().findFragmentById(R.id.playlist);
      if (!(frag instanceof SetFragment)) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.playlist, SetFragment.newInstance(mediaBrowser.getSessionToken()))
            .commit();
      }
      getLibraryFragment().notifyConnectionChanged();
    }

    @Override
    public void onConnectionSuspended() {
      Fragment frag = getSupportFragmentManager().findFragmentById(R.id.playlist);
      if (!(frag instanceof NoSetFragment)) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.playlist, new NoSetFragment()).commit();
      }
      getLibraryFragment().notifyConnectionChanged();
    }

    @Override
    public void onConnectionFailed() {
      super.onConnectionFailed();
    }
  };

  private LibraryFragment getLibraryFragment() {
    return ((LibraryFragment) getSupportFragmentManager().findFragmentById(R.id.library_fragment));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    monitorPlaylistSwitcher = (ViewSwitcher) findViewById(R.id.monitor_playlist_switcher);
    if (savedInstanceState != null) {
      if (monitorPlaylistSwitcher != null) {
        monitorPlaylistSwitcher.setDisplayedChild(savedInstanceState.getBoolean(STATE_SHOW_PLAYLIST)
            ? INDEX_PLAYLIST : INDEX_MONITOR);
      }
    }
    mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlaylistMediaService
        .class), playlistServiceConn, null);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    if (getSupportFragmentManager().findFragmentById(R.id.playlist) == null) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.playlist, NoSetFragment.newInstance()).commit();
    }

    Intent intent = new Intent(PlaylistMediaService.ACTION_CHECK_IS_ALIVE);
    intent.setPackage(getApplication().getPackageName());
    sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (getResultCode() == RESULT_OK) {
          mediaBrowser.connect();
        }
      }
    }, null, RESULT_CANCELED, null, null);
  }

  @Override
  protected void onDestroy() {
    mediaBrowser.disconnect();
    super.onDestroy();
  }

  @Override
  public void playMediaItemOnMonitor(@NonNull MediaItem mediaItem) {
    ((MonitorControlsFragment) getSupportFragmentManager()
        .findFragmentById(R.id.monitor_fragment)).playMedia(mediaItem);
  }

  @Override
  public boolean canAddToQueue() {
    return mediaBrowser.isConnected();
  }

  @Override
  public void addToQueue(@NonNull MediaItem mediaItem) {
    SetFragment setFragment = getSetlistFragment();
    if (setFragment != null) {
      setFragment.addToQueue(mediaItem);
    } else {
      Log.w(TAG, "addToQueue: no setlist fragment");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem showPlaylistToggle = menu.findItem(R.id.show_playlist);
    if (monitorPlaylistSwitcher == null) {
      showPlaylistToggle.setVisible(false);
    } else {
      showPlaylistToggle.setVisible(true);
      boolean showingPlaylist = monitorPlaylistSwitcher.getDisplayedChild() == INDEX_PLAYLIST;
      showPlaylistToggle.setChecked(showingPlaylist);
      ColorStateList tintList = getResources().getColorStateList(R.color.checkable_menu_item,
          getSupportActionBar().getThemedContext().getTheme());
      assert tintList != null;
      @ColorInt int tintColor = !showingPlaylist ? tintList.getColorForState(
          new int[]{android.R.attr.checked}, 0)
          : tintList.getDefaultColor();
      showPlaylistToggle.getIcon().setTint(tintColor);
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.show_playlist:
        boolean wasChecked = item.isChecked();
        if (monitorPlaylistSwitcher != null) {
          monitorPlaylistSwitcher.setDisplayedChild(wasChecked ? INDEX_MONITOR : INDEX_PLAYLIST);
        }
        invalidateOptionsMenu();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Nullable
  public SetFragment getSetlistFragment() {
    Fragment frag = getSupportFragmentManager().findFragmentById(R.id.playlist);
    if (frag instanceof SetFragment) {
      return ((SetFragment) frag);
    }
    return null;
  }

  @Override
  public void startNewSetlist() {
    mediaBrowser.connect();
  }

  @Override
  public void endSet() {
    mediaBrowser.disconnect();
    playlistServiceConn.onConnectionSuspended();
  }
}
