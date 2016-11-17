package com.keyboardr.bluejay;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.service.PlaylistService;
import com.keyboardr.bluejay.ui.NoSetFragment;
import com.keyboardr.bluejay.ui.monitor.LibraryFragment;
import com.keyboardr.bluejay.ui.monitor.MonitorControlsFragment;
import com.keyboardr.bluejay.ui.playlist.SetFragment;

public class PlaybackActivity extends AppCompatActivity implements LibraryFragment.LibraryFragmentHolder,
        NoSetFragment.Holder, SetFragment.Holder {

    private static final String STATE_SHOW_PLAYLIST = "showPlaylist";
    @Nullable
    private ViewSwitcher monitorPlaylistSwitcher;

    private static final int INDEX_MONITOR = 0;
    private static final int INDEX_PLAYLIST = 1;

    private static final String TAG = "PlaybackActivity";

    public boolean playlistConnected;
    private ServiceConnection playlistServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            playlistConnected = true;
            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.playlist);
            if (!(frag instanceof SetFragment)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.playlist, SetFragment.newInstance(iBinder)).commit();
            }
            getLibraryFragment().notifyConnectionChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            playlistConnected = false;
            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.playlist);
            if (!(frag instanceof NoSetFragment)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.playlist, new NoSetFragment()).commit();
            }
            bindService(new Intent(PlaybackActivity.this, PlaylistService.class), playlistServiceConn,
                    BIND_IMPORTANT);
            getLibraryFragment().notifyConnectionChanged();
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
        bindService(new Intent(this, PlaylistService.class), playlistServiceConn, BIND_IMPORTANT);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (getSupportFragmentManager().findFragmentById(R.id.playlist) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.playlist, NoSetFragment.newInstance()).commit();
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(playlistServiceConn);
        super.onDestroy();
    }

    @Override
    public void playMediaItemOnMonitor(@NonNull MediaItem mediaItem) {
        ((MonitorControlsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.monitor_fragment)).playMedia(mediaItem);
    }

    @Override
    public boolean canAddToQueue() {
        return playlistConnected;
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
            @ColorInt int tintColor = !showingPlaylist ? tintList.getColorForState(new int[]{android.R.attr.checked}, 0)
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
        Intent service = new Intent(this, PlaylistService.class);
        service.putExtra(PlaylistService.EXTRA_END_SET, false);
        startService(service);
    }

    @Override
    public void endSet() {
        Intent service = new Intent(this, PlaylistService.class);
        service.putExtra(PlaylistService.EXTRA_END_SET, true);
        startService(service);
    }
}
