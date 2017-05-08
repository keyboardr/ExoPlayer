package com.keyboardr.bluejay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.MediaConnectedEvent;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.service.PlaylistMediaService;
import com.keyboardr.bluejay.ui.BottomNavHolder;
import com.keyboardr.bluejay.ui.EditShortlistsActivity;
import com.keyboardr.bluejay.ui.MonitorEditorFragment;
import com.keyboardr.bluejay.ui.NoSetFragment;
import com.keyboardr.bluejay.ui.monitor.MonitorControlsFragment;
import com.keyboardr.bluejay.ui.monitor.library.LibraryFragment;
import com.keyboardr.bluejay.ui.playlist.SetFragment;
import com.keyboardr.bluejay.ui.shortlists.ShortlistEditorFragment;

public class PlaybackActivity extends AppCompatActivity implements LibraryFragment.Holder,
    NoSetFragment.Holder, SetFragment.Holder, BottomNavHolder, MonitorEditorFragment.Holder,
    ShortlistEditorFragment.Holder {

  @SuppressWarnings("PointlessBooleanExpression")
  private static final boolean DEBUG_BYPASS_QUEUE_DEDUPE = BuildConfig.DEBUG && false;

  private static final String STATE_SWITCHER_INDEX = "switcherIndex";
  @Nullable
  private ViewAnimator monitorPlaylistSwitcher;
  @Nullable
  private CheckedTextView monitorTab;
  @Nullable
  private CheckedTextView playlistTab;
  @Nullable
  private CheckedTextView historyTab;

  private static final int INDEX_MONITOR = 0;
  private static final int INDEX_PLAYLIST = 1;
  private static final int INDEX_HISTORY = 2;

  private static final String TAG = "PlaybackActivity";

  private MediaBrowserCompat mediaBrowser;

  private SetMetadata pendingMetadata;

  private MediaBrowserCompat.ConnectionCallback playlistServiceConn = new MediaBrowserCompat
      .ConnectionCallback() {
    @Override
    public void onConnected() {
      Fragment existingFrag = getSupportFragmentManager().findFragmentById(R.id.playlist);
      if (pendingMetadata != null) {
        Bundle extras = new Bundle();
        extras.putParcelable(PlaylistMediaService.EXTRA_SET_METADATA, pendingMetadata.toBundle());
        try {
          new MediaControllerCompat(PlaybackActivity.this, mediaBrowser.getSessionToken())
              .sendCommand(PlaylistMediaService.COMMAND_SET_METADATA, extras, null);
        } catch (RemoteException e) {
          throw new RuntimeException(e);
        }
      }
      if (!(existingFrag instanceof SetFragment)) {
        SetFragment newFragment = SetFragment.newInstance(mediaBrowser.getSessionToken());
        if (pendingMetadata != null) {
          // This was started as a result of calling #startNewSetlist(SetMetadata) so animate it
          ChangeBounds sharedTransition = new ChangeBounds();
          sharedTransition.setStartDelay(100);
          newFragment.setSharedElementEnterTransition(sharedTransition);
          newFragment.setEnterTransition(new Explode());
          existingFrag.setExitTransition(new Slide());
        }

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.playlist, newFragment)
            .addSharedElement(findViewById(R.id.new_setlist_container),
                getString(R.string.shared_element_bottom_bar))
            .commit();
      }
      pendingMetadata = null;
      Buses.PLAYLIST.postSticky(new MediaConnectedEvent(true));
    }

    @Override
    public void onConnectionSuspended() {
      Fragment existingFrag = getSupportFragmentManager().findFragmentById(R.id.playlist);
      if (!(existingFrag instanceof NoSetFragment)) {
        NoSetFragment newFragment = NoSetFragment.newInstance();
        newFragment.setSharedElementEnterTransition(new ChangeBounds());
        Slide enterSlide = new Slide();
        enterSlide.setStartDelay(100);
        newFragment.setEnterTransition(enterSlide);
        existingFrag.setExitTransition(new Explode());

        getSupportFragmentManager().beginTransaction()
            .addSharedElement(findViewById(R.id.set_info_fragment),
                getString(R.string.shared_element_setlist_container))
            .replace(R.id.playlist, newFragment).commit();
      }
      Buses.PLAYLIST.postSticky(new MediaConnectedEvent(false));
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
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Init switcher and tabs for phones
    monitorPlaylistSwitcher = (ViewAnimator) findViewById(R.id.monitor_playlist_switcher);
    monitorTab = (CheckedTextView) findViewById(R.id.bottom_tab_monitor);
    if (monitorTab != null) {
      monitorTab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          setDisplayedChild(INDEX_MONITOR);
        }
      });
    }
    playlistTab = (CheckedTextView) findViewById(R.id.bottom_tab_playlist);
    if (playlistTab != null) {
      playlistTab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          setDisplayedChild(INDEX_PLAYLIST);
        }
      });
    }
    historyTab = (CheckedTextView) findViewById(R.id.bottom_tab_history);
    if (historyTab != null) {
      historyTab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          setDisplayedChild(INDEX_HISTORY);
        }
      });
    }

    if (monitorPlaylistSwitcher != null) {
      if (savedInstanceState != null) {
        setDisplayedChild(savedInstanceState.getInt(STATE_SWITCHER_INDEX, INDEX_MONITOR));
      } else {
        setDisplayedChild(monitorPlaylistSwitcher.getDisplayedChild());
      }
    }

    mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlaylistMediaService
        .class), playlistServiceConn, null);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    Fragment playlistFragment = getSupportFragmentManager().findFragmentById(R.id.playlist);
    boolean isEditorFragment =
        playlistFragment instanceof MonitorEditorFragment || playlistFragment instanceof
            ShortlistEditorFragment;
    if (playlistFragment == null
        || (!getResources().getBoolean(R.bool.allow_library_editor) && isEditorFragment)) {
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
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (monitorPlaylistSwitcher != null) {
      outState.putInt(STATE_SWITCHER_INDEX, monitorPlaylistSwitcher.getDisplayedChild());
    }
  }

  @Override
  protected void onDestroy() {
    mediaBrowser.disconnect();
    super.onDestroy();
  }

  @Override
  public void playMediaItemOnMonitor(@NonNull MediaItem mediaItem) {
    getMonitorControlsFragment().playMedia(mediaItem);
  }

  private MonitorControlsFragment getMonitorControlsFragment() {
    return (MonitorControlsFragment) getSupportFragmentManager()
        .findFragmentById(R.id.monitor_fragment);
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
    if (historyTab != null) {
      historyTab.setChecked(child == INDEX_HISTORY);
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
  public void startNewSetlist(SetMetadata setMetadata) {
    mediaBrowser.connect();
    pendingMetadata = setMetadata;
  }

  @Override
  public void editMetadata() {
    MonitorEditorFragment newFragment = MonitorEditorFragment.newInstance();
    newFragment.setEnterTransition(new Slide(Gravity.BOTTOM));

    getSupportFragmentManager().findFragmentById(R.id.playlist)
        .setExitTransition(new Slide(Gravity.TOP));
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.playlist, newFragment).commitNow();
  }

  @Override
  public void editShortlists() {
    if (getResources().getBoolean(R.bool.allow_library_editor)) {
      ShortlistEditorFragment fragment = ShortlistEditorFragment.newInstance();
      fragment.setEnterTransition(
          TransitionInflater.from(this)
              .inflateTransition(R.transition.edit_shortlist_fragment_enter));

      getSupportFragmentManager().findFragmentById(R.id.playlist).setExitTransition(
          new Slide(Gravity.TOP));
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.playlist, fragment).commitNow();
    } else {
      startActivity(new Intent(this, EditShortlistsActivity.class));
    }
  }

  @Nullable
  @Override
  public MediaItem getCurrentMonitorTrack() {
    return getMonitorControlsFragment().getCurrentTrack();
  }

  @Override
  public void endSet() {
    mediaBrowser.disconnect();
    playlistServiceConn.onConnectionSuspended();
  }

  @Nullable
  @Override
  public View getPlaylistTabView() {
    return playlistTab;
  }

  @Override
  public void closeMetadataEditor() {
    NoSetFragment newFragment = NoSetFragment.newInstance();
    newFragment.setEnterTransition(new Slide(Gravity.TOP));

    getSupportFragmentManager().findFragmentById(R.id.playlist)
        .setExitTransition(new Slide(Gravity.BOTTOM));
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.playlist, newFragment).commitNow();
  }

  @Override
  public void closeShortlistEditor() {
    NoSetFragment newFragment = NoSetFragment.newInstance();
    newFragment.setEnterTransition(new Slide(Gravity.TOP));

    getSupportFragmentManager().findFragmentById(R.id.playlist).setExitTransition
        (TransitionInflater.from(this)
            .inflateTransition(R.transition.edit_shortlist_fragment_exit));

    getSupportFragmentManager().beginTransaction()
        .replace(R.id.playlist, newFragment).commitNow();
  }

  @Override
  public void onBackPressed() {
    Fragment editorFragment = getSupportFragmentManager().findFragmentById(R.id.playlist);
    if (editorFragment instanceof MonitorEditorFragment) {
      closeMetadataEditor();
    } else if (editorFragment instanceof ShortlistEditorFragment) {
      closeShortlistEditor();
    } else {
      super.onBackPressed();
    }
  }
}
