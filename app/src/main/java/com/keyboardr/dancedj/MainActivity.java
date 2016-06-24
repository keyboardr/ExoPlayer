package com.keyboardr.dancedj;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.keyboardr.dancedj.model.MediaItem;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity implements LibraryFragment.LibraryFragmentHolder{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void playMediaItemOnMonitor(@NonNull MediaItem mediaItem) {
        ((MonitorControlsFragment) getSupportFragmentManager().findFragmentById(R.id.monitor_fragment)).playMedia(mediaItem);
    }
}
