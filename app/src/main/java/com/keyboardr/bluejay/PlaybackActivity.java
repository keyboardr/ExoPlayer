package com.keyboardr.bluejay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistMediaService;
import com.keyboardr.bluejay.ui.BottomNavHolder;
import com.keyboardr.bluejay.ui.NoSetFragment;
import com.keyboardr.bluejay.ui.monitor.MonitorControlsFragment;
import com.keyboardr.bluejay.ui.monitor.library.LibraryFragment;
import com.keyboardr.bluejay.ui.playlist.SetFragment;

public class PlaybackActivity extends AppCompatActivity implements LibraryFragment.Holder,
    NoSetFragment.Holder, SetFragment.Holder, BottomNavHolder {

  @SuppressWarnings("PointlessBooleanExpression")
  private static final boolean DEBUG_BYPASS_QUEUE_DEDUPE = BuildConfig.DEBUG && false;

  private static final String STATE_SHOW_PLAYLIST = "showPlaylist";
  @Nullable
  private ViewSwitcher monitorPlaylistSwitcher;
  @Nullable
  private CheckedTextView monitorTab;
  @Nullable
  private CheckedTextView playlistTab;
  @Nullable
  private ImageView monitorTabBackground;
  @Nullable
  private ImageView playlistTabBackground;

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

    // Init switcher and tabs for phones
    monitorPlaylistSwitcher = (ViewSwitcher) findViewById(R.id.monitor_playlist_switcher);
    monitorTab = (CheckedTextView) findViewById(R.id.bottom_tab_monitor);
    if (monitorTab != null) {
      monitorTab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          setDisplayedChild(INDEX_MONITOR);
        }
      });
    }
    monitorTabBackground = (ImageView) findViewById(R.id.bottom_tab_monitor_bg);
    playlistTab = (CheckedTextView) findViewById(R.id.bottom_tab_playlist);
    if (playlistTab != null) {
      playlistTab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          setDisplayedChild(INDEX_PLAYLIST);
        }
      });
    }
    playlistTabBackground = (ImageView) findViewById(R.id.bottom_tab_playlist_bg);

    if (monitorPlaylistSwitcher != null) {
      if (savedInstanceState != null) {
        setDisplayedChild(savedInstanceState.getBoolean(STATE_SHOW_PLAYLIST)
            ? INDEX_PLAYLIST : INDEX_MONITOR);
      } else {
        setDisplayedChild(monitorPlaylistSwitcher.getDisplayedChild());
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
  public boolean queueContains(@NonNull MediaItem mediaItem) {
    SetFragment setlistFragment = getSetlistFragment();
    return !DEBUG_BYPASS_QUEUE_DEDUPE && setlistFragment != null
        && setlistFragment.queueContains(mediaItem);
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

  private void setDisplayedChild(int child) {
    if (monitorPlaylistSwitcher != null && child != monitorPlaylistSwitcher.getDisplayedChild()) {
      monitorPlaylistSwitcher.setDisplayedChild(child);
    }
    if (monitorTab != null) {
      monitorTab.setChecked(child == INDEX_MONITOR);
      getLibraryFragment().setHasOptionsMenu(monitorTab.isChecked());
    }
    if (playlistTab != null) {
      playlistTab.setChecked(child == INDEX_PLAYLIST);
    }
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

  @Override
  public void setMonitorAlbumArt(@Nullable Icon icon) {
    if (monitorTabBackground != null) {
      monitorTabBackground.setImageIcon(icon);
    }
  }

  @Override
  public void setPlaylistAlbumArt(@Nullable Icon icon) {
    if (playlistTabBackground != null) {
      playlistTabBackground.setImageIcon(icon);
    }
  }

  @Nullable
  @Override
  public View getPlaylistTabView() {
    return playlistTab;
  }
}
