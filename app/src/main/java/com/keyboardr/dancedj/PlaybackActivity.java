package com.keyboardr.dancedj;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ViewSwitcher;

import com.keyboardr.dancedj.model.MediaItem;

public class PlaybackActivity extends AppCompatActivity implements LibraryFragment.LibraryFragmentHolder {

    private static final String STATE_SHOW_PLAYLIST = "showPlaylist";
    @Nullable
    private ViewSwitcher monitorPlaylistSwitcher;

    private static final int INDEX_MONITOR = 0;
    private static final int INDEX_PLAYLIST = 1;

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
    }

    @Override
    public void playMediaItemOnMonitor(@NonNull MediaItem mediaItem) {
        ((MonitorControlsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.monitor_fragment)).playMedia(mediaItem);
    }

    @Override
    public void addToQueue(@NonNull MediaItem mediaItem) {
        ((PlaylistControlsFragment) getSupportFragmentManager().findFragmentById(R.id.playlist_control_fragment)).addToQueue(mediaItem);
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
}
